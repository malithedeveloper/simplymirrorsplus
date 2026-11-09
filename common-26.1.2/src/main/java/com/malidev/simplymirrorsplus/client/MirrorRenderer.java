package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.malidev.simplymirrorsplus.block.*;
import com.malidev.simplymirrorsplus.math.*;
import com.malidev.simplymirrorsplus.client.mixin.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import java.util.*;
import java.util.function.Predicate;
import static org.lwjgl.opengl.GL33.*;

/** Planar reflections on the 26.1.2 extracted-state renderer. OpenGL compositor, no portal runtime. */
public final class MirrorRenderer {
    private static final int MAX_PLANES=6;
    private static final int MAX_CACHED_PLANES=8;
    /** Spreads refreshes over frames so a room full of mirrors cannot spike one frame. */
    private static final int MAX_PLANE_UPDATES_PER_FRAME=2;
    /** 1 = plain reflection, 2 = a mirror visible inside a reflection also reflects. */
    private static final int MAX_DEPTH=2;

    public static RenderTarget redirect;
    public static long completedPasses;
    public static Matrix4fc reflectionProjection;
    public static final Matrix4f mainProjection=new Matrix4f();

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
    private static ProjectionMatrixBuffer projectionBuffer;
    private static Level lastLevel;
    private static List<MirrorSurface> cached=new ArrayList<>();
    private static int frame,compositeFbo;
    private static boolean failed;

    private static final class Slot {
        final MirrorRefreshClock clock=new MirrorRefreshClock();
        TextureTarget target;
        List<MirrorSurface> faces;
        int seenFrame=-2;
        float[] capturedViewProjection;
        Vec3 capturedEye;
    }
    private record Frame(GraphicsResourceAllocator allocator,DeltaTracker delta,float partial,Matrix4f perspective,MirrorTemporalHistory.Frame prior,GpuBufferSlice fog,Vector4f color,boolean sky,LevelRenderState state,boolean sodium) { }
    private record View(Camera camera,Vec3 eye,Matrix4f view,Matrix4f projection,RenderTarget target,int depth) {
        float[] viewProjection() { return MirrorMatrices.multiply(projection.get(new float[16]),view.get(new float[16])); }
    }

    private MirrorRenderer() { }

    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,CameraRenderState camera,
            Matrix4fc mainViewMatrix,GpuBufferSlice fog,Vector4f color,boolean sky) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(!MirrorConfig.enabled) { releaseTargets();return; }
        RenderTarget main=mc.getMainRenderTarget();
        if(!(main.getColorTexture() instanceof GlTexture))return;
        if(mc.level!=lastLevel) { release();lastLevel=mc.level; }
        Vec3 eye=camera.pos;
        int thisFrame=frame++;
        if(thisFrame%20==0)cached=find(mc.level,eye);
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Matrix4f savedView=new Matrix4f(mainViewMatrix);
        // Use the exact world projection captured before the GPU upload.
        Matrix4f savedProjection=new Matrix4f(camera.projectionMatrix);
        Matrix4f renderedProjection=new Matrix4f(mainProjection);
        float[] view=savedView.get(new float[16]),proj=renderedProjection.get(new float[16]);
        MirrorTemporalHistory.Frame prior=TEMPORAL.advance(view,proj,eye.x,eye.y,eye.z);
        if(cached.isEmpty()||failed)return;
        cached.sort(Comparator.comparingDouble(s->distance(s,eye)));
        View root=new View(mc.gameRenderer.getMainCamera(),eye,savedView,renderedProjection,main,0);
        Map<String,List<MirrorSurface>> visiblePlanes=visiblePlanes(mc,root,s->true);
        if(visiblePlanes.isEmpty())return;
        float[] vp=root.viewProjection();
        LevelRendererAccessor access=(LevelRendererAccessor)mc.levelRenderer;
        LevelRenderState state=access.simplymirrorsplus$state();
        GpuBufferSlice savedProj=RenderSystem.getProjectionMatrixBuffer(),savedFog=RenderSystem.getShaderFog();
        ProjectionType savedType=RenderSystem.getProjectionType();
        GpuBuffer savedGlobals=RenderSystem.getGlobalSettingsUniform();
        boolean oldSmartCull=mc.smartCull;
        boolean sodium=ShaderReflection.terrainRendererPresent();
        boolean shaders=ShaderReflection.enabled();
        var visible=mc.levelRenderer.getVisibleSections();
        var savedVisible=new ArrayList<>(visible);
        // Iris sizes its gbuffers from the depth target; a scaled mirror would make it reallocate them every pass.
        int percent=shaders?100:MirrorConfig.resolutionPercent;
        int width=Math.max(1,main.width*percent/100),height=Math.max(1,main.height*percent/100);
        List<Slot> planes=new ArrayList<>();
        for(Map.Entry<String,List<MirrorSurface>> plane:visiblePlanes.entrySet()) {
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
        Frame context=new Frame(allocator,delta,partial,renderedProjection,prior,fog,color,sky,state,sodium);
        boolean captured=false;
        try {
            if(projectionBuffer==null)projectionBuffer=new ProjectionMatrixBuffer("Simply Mirrors Plus projection");
            int updates=0;
            for(Slot slot:planes) {
                if(updates==MAX_PLANE_UPDATES_PER_FRAME||!slot.clock.isDue(now))continue;
                back=fit(back,width,height);
                captured=true;
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
                composite(main,glId(slot.target),vp,eye,slot.capturedViewProjection,slot.capturedEye,slot.faces);
            }
        } catch(RuntimeException exception) {
            failed=true;java.util.logging.Logger.getLogger("simplymirrorsplus").log(java.util.logging.Level.SEVERE,"Mirror reflection disabled for this world after a rendering error",exception);
        } finally {
            mc.smartCull=oldSmartCull;reflectionProjection=null;
            redirect=null;RenderSystem.setGlobalSettingsUniform(savedGlobals);RenderSystem.setProjectionMatrix(savedProj,savedType);RenderSystem.setShaderFog(savedFog);
            if(captured) {
                ShaderReflection.end();
                visible.clear();visible.addAll(savedVisible);
                if(sodium&&!shaders)mc.levelRenderer.update(mc.gameRenderer.getMainCamera());
                state.reset();mc.levelRenderer.extractLevel(delta,mc.gameRenderer.getMainCamera(),partial);
                mc.gameRenderer.getMainCamera().extractRenderState(state.cameraRenderState,partial);
                state.cameraRenderState.viewRotationMatrix.set(savedView);state.cameraRenderState.projectionMatrix.set(savedProjection);
            }
        }
    }

    /** Renders what {@code surface} shows from {@code parent} into {@code into}. */
    private static void capture(Frame frame,View parent,MirrorSurface surface,TextureTarget into) {
        Minecraft mc=Minecraft.getInstance();
        boolean shaders=ShaderReflection.enabled();
        boolean players=MirrorConfig.playersOnly;
        double[] reflected=surface.plane().reflectPoint(parent.eye.x,parent.eye.y,parent.eye.z);
        Vec3 eye=new Vec3(reflected[0],reflected[1],reflected[2]);
        float[] parentView=parent.view.get(new float[16]),perspective=frame.perspective.get(new float[16]);
        float[] viewValues=MirrorMatrices.multiply(parentView,ReflectionTransform.matrix(surface.nx,surface.ny,surface.nz));
        Matrix4f view=new Matrix4f().set(viewValues);
        double offsetX=surface.cx-reflected[0],offsetY=surface.cy-reflected[1],offsetZ=surface.cz-reflected[2];
        Matrix4f projection=shaders?new Matrix4f(frame.perspective):new Matrix4f().set(MirrorMatrices.oblique(perspective,viewValues,surface.nx,surface.ny,surface.nz,offsetX,offsetY,offsetZ,RenderSystem.getDevice().isZZeroToOne()));
        Camera virtual=new ReflectionCamera(view);
        virtual.setLevel(mc.level);virtual.setEntity(mc.getCameraEntity());virtual.update(frame.delta);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parent.camera.yRot():surface.nz!=0?180-parent.camera.yRot():-parent.camera.yRot(),surface.ny!=0?-parent.camera.xRot():parent.camera.xRot());
        accessor.simplymirrorsplus$detached(true);
        virtual.tick();
        accessor.simplymirrorsplus$frustum(view,frame.perspective,eye);
        View self=new View(virtual,eye,view,projection,into,parent.depth+1);
        LevelRenderState state=frame.state;
        var visible=mc.levelRenderer.getVisibleSections();
        if(frame.sodium) {
            // Sodium owns its section lists; updating vanilla visibleSections alone has no effect.
            redirect=into;reflectionProjection=projection;mc.smartCull=false;
            if(!shaders&&!players)mc.levelRenderer.update(virtual);
        }
        visible.clear();
        if(!players) {
            // Use loaded section geometry directly: the main camera's occlusion graph may
            // omit a sealed room on the opposite side of the mirror wall.
            var area=((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$viewArea();
            if(area!=null)for(var section:area.sections)
                if(virtual.getCullFrustum().isVisible(section.getBoundingBox()))visible.add(section);
        }
        state.reset();mc.levelRenderer.extractLevel(frame.delta,virtual,frame.partial);virtual.extractRenderState(state.cameraRenderState,frame.partial);
        state.cameraRenderState.viewRotationMatrix.set(view);state.cameraRenderState.projectionMatrix.set(projection);
        state.weatherRenderState.reset();state.particlesRenderState.reset();
        if(players)keepOnlyPlayersOverEndSky(state);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),players?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
        int oldFront=glGetInteger(GL_FRONT_FACE);
        var modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        boolean shaderPass=shaders&&!players;
        try(GpuBuffer globals=globals(eye,into.width,into.height,mc,frame.partial)) {
            redirect=into;
            if(shaderPass)ShaderReflection.begin(perspective,viewValues,surface.nx,surface.ny,surface.nz,offsetX,offsetY,offsetZ,virtual,parentView,new double[]{parent.eye.x,parent.eye.y,parent.eye.z},reflected,frame.prior.reflect(surface));
            RenderSystem.setGlobalSettingsUniform(globals);
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
            glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
            mc.levelRenderer.renderLevel(frame.allocator,frame.delta,false,state.cameraRenderState,view,frame.fog,new Vector4f(frame.color),players||frame.sky,state.chunkSectionsToRender);
            // ponytail: Iris reflection state is single-level, so shader packs stop at one bounce.
            if(!shaders&&!players)renderMirrorsInReflection(frame,self,surface,into);
        } finally {
            if(shaderPass)ShaderReflection.end();
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:parent.target;
            modelView.popMatrix();
        }
    }

    /** Skips the world by editing the extracted state: only player models and the End sky stay. */
    private static void keepOnlyPlayersOverEndSky(LevelRenderState state) {
        state.entityRenderStates.removeIf(entity->!(entity instanceof AvatarRenderState));
        for(EntityRenderState entity:state.entityRenderStates) {
            // Shadows are projected onto world blocks, which are not drawn here.
            entity.shadowRadius=0;
            entity.shadowPieces.clear();
        }
        state.blockEntityRenderStates.clear();
        state.blockBreakingRenderStates.clear();
        state.blockOutlineRenderState=null;
        state.worldBorderRenderState.reset();
        state.haveGlowingEntities=false;
        state.skyRenderState.reset();
        state.skyRenderState.skybox=DimensionType.Skybox.END;
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
                texture=glId(NESTED[index]);
            } else {
                Slot slot=SLOTS.get(plane.getKey());
                if(slot==null||slot.capturedViewProjection==null)continue;
                // ponytail: this image was taken from the main camera, so its perspective is approximate at this depth.
                texture=glId(slot.target);
                sourceViewProjection=slot.capturedViewProjection;
                sourceEye=slot.capturedEye;
            }
            composite(into,texture,vp,eye,sourceViewProjection,sourceEye,plane.getValue());
        }
    }

    private static void composite(RenderTarget into,int texture,float[] vp,Vec3 eye,float[] sourceViewProjection,Vec3 sourceEye,List<MirrorSurface> faces) {
        int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFbo==0)compositeFbo=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFbo);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,((GlTexture)into.getColorTexture()).glId(),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,((GlTexture)into.getDepthTexture()).glId(),0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mirror composite framebuffer incomplete");
            glViewport(0,0,into.width,into.height);
            COMPOSITE.drawMergedBatch(texture,vp,eye.x,eye.y,eye.z,sourceViewProjection,sourceEye.x,sourceEye.y,sourceEye.z,faces);
        } finally { glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw); }
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

    private static int glId(TextureTarget target) { return ((GlTexture)target.getColorTexture()).glId(); }

    private static TextureTarget fit(TextureTarget target,int width,int height) {
        if(target!=null&&target.width==width&&target.height==height)return target;
        if(target!=null)target.destroyBuffers();
        target=new TextureTarget("Simply Mirrors Plus reflection",width,height,true);
        // Reprojected and downscaled images do not land on texel centres.
        int bound=glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D,glId(target));
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D,bound);
        return target;
    }

    /** Terrain draw extraction queries the Camera directly instead of the renderLevel matrix. */
    private static final class ReflectionCamera extends Camera {
        private final Matrix4f reflectedView;
        ReflectionCamera(Matrix4f reflectedView) { this.reflectedView=new Matrix4f(reflectedView); }
        @Override public Matrix4f getViewRotationMatrix(Matrix4f destination) { return destination.set(reflectedView); }
    }
    private static GpuBuffer globals(Vec3 eye,int width,int height,Minecraft mc,float partial) {
        int x=Mth.floor(eye.x),y=Mth.floor(eye.y),z=Mth.floor(eye.z);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            var data=Std140Builder.onStack(stack,GlobalSettingsUniform.UBO_SIZE).putIVec3(x,y,z)
                .putVec3((float)(x-eye.x),(float)(y-eye.y),(float)(z-eye.z)).putVec2(width,height).putFloat(1)
                .putFloat(((mc.level.getGameTime()%24000)+partial)/24000f).putInt(0).putInt(1).get();
            return RenderSystem.getDevice().createBuffer(()->"Simply Mirrors Plus camera",GpuBuffer.USAGE_UNIFORM,data);
        }
    }
    private static double distance(MirrorSurface s,Vec3 eye) {double x=s.cx-eye.x,y=s.cy-eye.y,z=s.cz-eye.z;return x*x+y*y+z*z;}
    private static List<MirrorSurface> find(Level level,Vec3 eye) {
        List<MirrorSurface> out=new ArrayList<>();int cx=(int)Math.floor(eye.x/16),cz=(int)Math.floor(eye.z/16);
        for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++) {
            if(!level.hasChunk(x,z))continue;
            for(BlockEntity be:level.getChunk(x,z).getBlockEntities().values()) {
                if(!(be instanceof MirrorBlockEntity))continue;BlockPos pos=be.getBlockPos();BlockState state=level.getBlockState(pos);
                if(!(state.getBlock() instanceof MirrorBlock))continue;
                for(MirrorSurface face:MirrorSurface.faces(pos.getX(),pos.getY(),pos.getZ()))
                    if(!(level.getBlockState(new BlockPos(face.x+face.nx,face.y+face.ny,face.z+face.nz)).getBlock() instanceof MirrorBlock))out.add(face);
            }
        }
        out.sort(Comparator.comparingDouble(s->distance(s,eye)));return out;
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
        if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        if(compositeFbo!=0)glDeleteFramebuffers(compositeFbo);
        compositeFbo=0;
        COMPOSITE.close();TEMPORAL.reset();redirect=null;cached.clear();lastLevel=null;failed=false;frame=0;
    }
}
