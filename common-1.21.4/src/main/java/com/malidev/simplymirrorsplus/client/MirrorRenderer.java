package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorFaceSelector;
import com.malidev.simplymirrorsplus.math.MirrorMatrices;
import com.malidev.simplymirrorsplus.math.MirrorRefreshClock;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.malidev.simplymirrorsplus.math.MirrorTemporalHistory;
import com.malidev.simplymirrorsplus.math.ReflectionTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import static org.lwjgl.opengl.GL33.*;

/** Bounded, same-dimension planar reflection renderer. */
public final class MirrorRenderer {
    private static final int MAX_PLANES=6;
    private static final int MAX_CACHED_PLANES=8;
    /** Spreads refreshes over frames so a room full of mirrors cannot spike one frame. */
    private static final int MAX_PLANE_UPDATES_PER_FRAME=2;
    /** 1 = plain reflection, 2 = a mirror visible inside a reflection also reflects. */
    private static final int MAX_DEPTH=2;

    public static RenderTarget redirect;
    public static long completedPasses;

    private static final GlMirrorComposite COMPOSITE=new GlMirrorComposite();
    private static final MirrorTemporalHistory TEMPORAL=new MirrorTemporalHistory();
    /** Each plane owns its image so frames between refreshes reuse it instead of re-rendering. */
    private static final Map<String,Slot> SLOTS=new LinkedHashMap<>(16,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Slot> eldest) {
            if(size()<=MAX_CACHED_PLANES)return false;
            if(eldest.getValue().target!=null)eldest.getValue().target.destroyBuffers();
            return true;
        }
    };
    private static final TextureTarget[] NESTED=new TextureTarget[MAX_DEPTH-1];
    /** Render destination swapped into a slot when finished, so a slot always holds a complete image. */
    private static TextureTarget back;
    private static Level lastLevel;
    private static List<MirrorSurface> cached=new ArrayList<>();
    private static int frame;
    private static boolean failed;

    private static final class Slot {
        final MirrorRefreshClock clock=new MirrorRefreshClock();
        TextureTarget target;
        List<MirrorSurface> faces;
        int seenFrame=-2;
        float[] capturedViewProjection;
        Vec3 capturedEye;
    }
    private record Frame(GraphicsResourceAllocator allocator,DeltaTracker delta,GameRenderer game,float partial,Matrix4f perspective,MirrorTemporalHistory.Frame prior) { }
    private record View(Camera camera,Vec3 eye,Matrix4f view,Matrix4f projection,RenderTarget target,int depth) {
        float[] viewProjection() { return MirrorMatrices.multiply(array(projection),array(view)); }
    }

    private MirrorRenderer() { }

    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,GameRenderer game,Matrix4f mainView,Matrix4f mainProjection) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(!MirrorConfig.enabled) { releaseTargets();return; }
        Vec3 eye=camera.getPosition();
        if(mc.level!=lastLevel) { release();lastLevel=mc.level; }
        int thisFrame=frame++;
        if(thisFrame%20==0)cached=find(mc.level,eye);
        float[] view=array(mainView),proj=array(mainProjection);
        MirrorTemporalHistory.Frame prior=TEMPORAL.advance(view,proj,eye.x,eye.y,eye.z);
        if(cached.isEmpty()||failed)return;
        cached.sort(Comparator.comparingDouble(s->distance(s,eye)));
        RenderTarget main=mc.getMainRenderTarget();
        View root=new View(camera,eye,mainView,mainProjection,main,0);
        Frame context=new Frame(allocator,delta,game,delta.getGameTimeDeltaPartialTick(false),mainProjection,prior);
        float[] vp=root.viewProjection();
        boolean shaders=ShaderReflection.enabled();
        // Iris sizes its gbuffers from the depth target; a scaled mirror would make it reallocate them every pass.
        int percent=shaders?100:MirrorConfig.resolutionPercent;
        int width=Math.max(1,main.width*percent/100),height=Math.max(1,main.height*percent/100);
        List<Slot> planes=new ArrayList<>();
        for(Map.Entry<String,List<MirrorSurface>> plane:visiblePlanes(mc,root,s->true).entrySet()) {
            if(planes.size()==MAX_PLANES)break;
            Slot slot=SLOTS.computeIfAbsent(plane.getKey(),k->new Slot());
            slot.faces=plane.getValue();
            boolean wasVisible=slot.seenFrame==thisFrame-1;
            slot.seenFrame=thisFrame;
            boolean resized=slot.target==null||slot.target.width!=width||slot.target.height!=height;
            if(resized)slot.capturedViewProjection=null;
            if(resized||!wasVisible)slot.clock.reset();
            slot.target=fit(slot.target,width,height);
            planes.add(slot);
        }
        planes.sort(Comparator.comparingLong(s->s.clock.due()));
        long now=System.nanoTime(),interval=1_000_000_000L/MirrorConfig.refreshRate;
        try {
            int updates=0;
            for(Slot slot:planes) {
                if(updates==MAX_PLANE_UPDATES_PER_FRAME||!slot.clock.isDue(now))continue;
                back=fit(back,width,height);
                if(shaders&&DepthTextureParity.match(main.getDepthTextureId(),back.getDepthTextureId(),back.width,back.height))
                    java.util.logging.Logger.getLogger("simplymirrorsplus").info("Matched mirror depth internal format to Iris main target");
                capture(context,root,slot.faces.get(0),back);
                TextureTarget finished=back;
                back=slot.target;
                slot.target=finished;
                slot.capturedViewProjection=vp;
                slot.capturedEye=eye;
                slot.clock.rendered(now,interval);
                updates++;
                completedPasses++;
            }
            for(Slot slot:planes) {
                if(slot.capturedViewProjection==null)continue;
                Vec3 c=slot.capturedEye;
                COMPOSITE.drawMergedBatch(slot.target.getColorTextureId(),vp,eye.x,eye.y,eye.z,slot.capturedViewProjection,c.x,c.y,c.z,slot.faces);
            }
        } catch(RuntimeException exception) {
            failed=true;
            java.util.logging.Logger.getLogger("simplymirrorsplus").log(java.util.logging.Level.SEVERE,"Mirror reflection disabled for this world after a rendering error",exception);
        } finally {
            redirect=null;
            main.bindWrite(true);
            RenderSystem.setProjectionMatrix(mainProjection,ProjectionType.PERSPECTIVE);
        }
    }

    /** Renders what {@code surface} shows from {@code parent} into {@code into}, restoring the parent's GL state afterwards. */
    private static void capture(Frame frame,View parent,MirrorSurface surface,TextureTarget into) {
        Minecraft mc=Minecraft.getInstance();
        boolean shaders=ShaderReflection.enabled();
        double[] reflected=surface.plane().reflectPoint(parent.eye.x,parent.eye.y,parent.eye.z);
        Vec3 eye=new Vec3(reflected[0],reflected[1],reflected[2]);
        float[] parentView=array(parent.view),perspective=array(frame.perspective);
        float[] viewValues=MirrorMatrices.multiply(parentView,ReflectionTransform.matrix(surface.nx,surface.ny,surface.nz));
        Matrix4f view=matrix(viewValues);
        double offsetX=surface.cx-reflected[0],offsetY=surface.cy-reflected[1],offsetZ=surface.cz-reflected[2];
        Matrix4f projection=shaders?frame.perspective:matrix(MirrorMatrices.oblique(perspective,viewValues,surface.nx,surface.ny,surface.nz,offsetX,offsetY,offsetZ,false));
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,frame.partial);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parent.camera.getYRot():surface.nz!=0?180-parent.camera.getYRot():-parent.camera.getYRot(),surface.ny!=0?-parent.camera.getXRot():parent.camera.getXRot());
        View self=new View(virtual,eye,view,projection,into,parent.depth+1);
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull;
        boolean oldDepthMask=glGetBoolean(GL_DEPTH_WRITEMASK);
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        redirect=into;
        try {
            // Iris final compositing leaves depth writes disabled; clears obey that mask.
            RenderSystem.depthMask(true);
            if(MirrorConfig.playersOnly)into.setClearColor(0,0,0,1);else into.setClearColor(0.615f,0.718f,0.792f,1);
            into.clear();
            into.bindWrite(true);
            boolean shaderPass=shaders&&!MirrorConfig.playersOnly;
            if(shaderPass)ShaderReflection.begin(perspective,viewValues,surface.nx,surface.ny,surface.nz,offsetX,offsetY,offsetZ,virtual,parentView,new double[]{parent.eye.x,parent.eye.y,parent.eye.z},reflected,frame.prior.reflect(surface));
            try {
                RenderSystem.setProjectionMatrix(projection,ProjectionType.PERSPECTIVE);
                glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
                if(MirrorConfig.playersOnly) {
                    renderPlayersOnEndSky(frame,self);
                } else {
                    // Vanilla camera-cube fitting can loop forever with an oblique near plane.
                    // Cull conservatively with perspective; retain oblique projection for drawing.
                    mc.levelRenderer.prepareCullFrustum(eye,view,frame.perspective);
                    mc.levelRenderer.renderLevel(frame.allocator,frame.delta,false,virtual,frame.game,view,projection);
                }
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            // ponytail: Iris reflection state is single-level, so shader packs stop at one bounce.
            if(!shaders&&!MirrorConfig.playersOnly)renderMirrorsInReflection(frame,self,surface,into);
        } finally {
            RenderSystem.depthMask(oldDepthMask);
            mc.smartCull=oldSmartCull;
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:parent.target;
            parent.target.bindWrite(true);
            RenderSystem.setProjectionMatrix(parent.projection,ProjectionType.PERSPECTIVE);
            mc.levelRenderer.prepareCullFrustum(parent.eye,parent.view,frame.perspective);
            mc.getEntityRenderDispatcher().prepare(mc.level,parent.camera,mc.crosshairPickEntity);
        }
    }

    /**
     * Without this a mirror seen inside a reflection shows its block texture. The nearest plane gets a real
     * bounce; the rest, and the last bounce, reuse images that already exist so they cost no world render.
     */
    private static void renderMirrorsInReflection(Frame frame,View self,MirrorSurface parentSurface,TextureTarget into) {
        Minecraft mc=Minecraft.getInstance();
        float[] vp=self.viewProjection();
        Vec3 eye=self.eye;
        boolean realBounce=self.depth<MAX_DEPTH;
        // World rendering can leave another target's viewport set, which would misplace these draws.
        into.bindWrite(true);
        // Geometry behind the parent mirror was clipped away, so nothing there may be composited.
        for(Map.Entry<String,List<MirrorSurface>> plane:visiblePlanes(mc,self,s->parentSurface.frontFacing(s.cx,s.cy,s.cz,.01)).entrySet()) {
            int texture;
            float[] sourceViewProjection=vp;
            Vec3 sourceEye=eye;
            if(realBounce) {
                realBounce=false;
                int index=self.depth-1;
                NESTED[index]=fit(NESTED[index],Math.max(16,into.width/2),Math.max(16,into.height/2));
                capture(frame,self,plane.getValue().get(0),NESTED[index]);
                texture=NESTED[index].getColorTextureId();
            } else {
                Slot slot=SLOTS.get(plane.getKey());
                if(slot==null||slot.capturedViewProjection==null)continue;
                // ponytail: this image was taken from the main camera, so its perspective is approximate at this depth.
                texture=slot.target.getColorTextureId();
                sourceViewProjection=slot.capturedViewProjection;
                sourceEye=slot.capturedEye;
            }
            COMPOSITE.drawMergedBatch(texture,vp,eye.x,eye.y,eye.z,sourceViewProjection,sourceEye.x,sourceEye.y,sourceEye.z,plane.getValue());
        }
    }

    /** VRChat-style pass: skips world rendering entirely, so it costs a sky draw plus the player models. */
    private static void renderPlayersOnEndSky(Frame frame,View self) {
        Minecraft mc=Minecraft.getInstance();
        Vec3 eye=self.eye;
        EntityRenderDispatcher dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.level,self.camera,mc.crosshairPickEntity);
        Matrix4fStack modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(self.view);
        // Shadows are projected onto world blocks, which are not drawn here.
        dispatcher.setRenderShadow(false);
        try {
            RenderSystem.setShaderFog(FogParameters.NO_FOG);
            ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$skyRenderer().renderEndSky();
            Lighting.setupLevel();
            PoseStack pose=new PoseStack();
            MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
            for(AbstractClientPlayer player:mc.level.players()) {
                if(!player.shouldRender(eye.x,eye.y,eye.z))continue;
                float partial=frame.delta.getGameTimeDeltaPartialTick(!mc.level.tickRateManager().isEntityFrozen(player));
                dispatcher.render(player,Mth.lerp(partial,player.xOld,player.getX())-eye.x,Mth.lerp(partial,player.yOld,player.getY())-eye.y,Mth.lerp(partial,player.zOld,player.getZ())-eye.z,partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
            }
            buffers.endBatch();
        } finally {
            dispatcher.setRenderShadow(true);
            modelView.popMatrix();
        }
    }

    private static Map<String,List<MirrorSurface>> visiblePlanes(Minecraft mc,View view,Predicate<MirrorSurface> filter) {
        BlockPos.MutableBlockPos queryPos=new BlockPos.MutableBlockPos();
        // Do not let nearer back-facing or off-screen tiles consume the visible budget.
        List<MirrorSurface> selected=MirrorFaceSelector.select(cached,view.viewProjection(),view.eye.x,view.eye.y,view.eye.z,
                MirrorFaceSelector.MAX_VISIBLE_FACES,s->filter.test(s)
                    && mc.level.getBlockState(queryPos.set(s.x,s.y,s.z)).getBlock() instanceof MirrorBlock
                    && !(mc.level.getBlockState(queryPos.set(s.x+s.nx,s.y+s.ny,s.z+s.nz)).getBlock() instanceof MirrorBlock));
        Map<String,List<MirrorSurface>> planes=new LinkedHashMap<>();
        for(MirrorSurface s:selected)planes.computeIfAbsent(s.planeKey(),k->new ArrayList<>()).add(s);
        return planes;
    }

    private static TextureTarget fit(TextureTarget target,int width,int height) {
        if(target!=null&&target.width==width&&target.height==height)return target;
        if(target==null)target=new TextureTarget(width,height,true);else target.resize(width,height);
        // Reprojected and downscaled images do not land on texel centres.
        target.setFilterMode(GL_LINEAR);
        return target;
    }

    private static double distance(MirrorSurface s,Vec3 eye) { double x=s.cx-eye.x,y=s.cy-eye.y,z=s.cz-eye.z;return x*x+y*y+z*z; }

    private static List<MirrorSurface> find(Level level,Vec3 eye) {
        List<MirrorSurface> surfaces=new ArrayList<>();
        int cx=(int)Math.floor(eye.x/16),cz=(int)Math.floor(eye.z/16);
        for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++) {
            if(!level.hasChunk(x,z))continue;
            for(BlockEntity be:level.getChunk(x,z).getBlockEntities().values()) {
                if(!(be instanceof MirrorBlockEntity))continue;
                BlockPos pos=be.getBlockPos();
                if(!(level.getBlockState(pos).getBlock() instanceof MirrorBlock))continue;
                for(MirrorSurface face:MirrorSurface.faces(pos.getX(),pos.getY(),pos.getZ()))
                    if(!(level.getBlockState(new BlockPos(face.x+face.nx,face.y+face.ny,face.z+face.nz)).getBlock() instanceof MirrorBlock))surfaces.add(face);
            }
        }
        surfaces.sort(Comparator.comparingDouble(s->distance(s,eye)));
        return surfaces;
    }

    private static void releaseTargets() {
        for(Slot slot:SLOTS.values())if(slot.target!=null)slot.target.destroyBuffers();
        SLOTS.clear();
        for(int i=0;i<NESTED.length;i++) { if(NESTED[i]!=null)NESTED[i].destroyBuffers();NESTED[i]=null; }
        if(back!=null)back.destroyBuffers();
        back=null;
    }

    public static void release() {
        releaseTargets();
        redirect=null;cached.clear();lastLevel=null;frame=0;failed=false;TEMPORAL.reset();COMPOSITE.close();
    }

    private static float[] array(Matrix4f matrix) { return matrix.get(new float[16]); }
    private static Matrix4f matrix(float[] values) { return new Matrix4f().set(values); }
}
