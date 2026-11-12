package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL33.*;

/** Minecraft 1.21.5 side of the mirror pipeline; {@link MirrorEngine} decides what to render. */
public final class MirrorRenderer implements MirrorEngine.Platform<TextureTarget> {
    public static RenderTarget redirect;
    private static final MirrorRenderer PLATFORM=new MirrorRenderer();
    private static final MirrorEngine<TextureTarget> ENGINE=new MirrorEngine<>(PLATFORM);
    private static final GlMirrorComposite COMPOSITE=new GlMirrorComposite();
    private static int compositeFbo;
    private final BlockPos.MutableBlockPos query=new BlockPos.MutableBlockPos();
    private GraphicsResourceAllocator allocator;
    private DeltaTracker delta;
    private GameRenderer game;

    private MirrorRenderer() { }

    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,GameRenderer game,Matrix4f mainView,Matrix4f mainProjection) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        if(!(GpuTextures.unwrap(main.getColorTexture()) instanceof GlTexture))return;
        PLATFORM.allocator=allocator;
        PLATFORM.delta=delta;
        PLATFORM.game=game;
        Vec3 eye=camera.getPosition();
        MirrorEngine.Pass root=new MirrorEngine.Pass(camera,new double[]{eye.x,eye.y,eye.z},array(mainView),array(mainProjection),0);
        root.target=main;
        try {
            ENGINE.render(root,main.width,main.height,mc.level,System.nanoTime());
        } finally {
            redirect=null;
            RenderSystem.setProjectionMatrix(mainProjection,ProjectionType.PERSPECTIVE);
        }
    }

    @Override public TextureTarget createTarget(int width,int height) {
        TextureTarget target=new TextureTarget("Simply Mirrors Plus reflection",width,height,true);
        // Reprojected and downscaled images do not land on texel centres.
        target.setFilterMode(FilterMode.LINEAR);
        return target;
    }

    @Override public void destroyTarget(TextureTarget target) { target.destroyBuffers(); }
    @Override public int width(TextureTarget target) { return target.width; }
    @Override public int height(TextureTarget target) { return target.height; }
    @Override public boolean shadersActive() { return ShaderReflection.enabled(); }
    @Override public boolean depthZeroToOne() { return false; }

    @Override public List<MirrorSurface> findSurfaces(double x,double y,double z) {
        Level level=Minecraft.getInstance().level;
        List<MirrorSurface> surfaces=new ArrayList<>();
        int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
        for(int chunkX=cx-2;chunkX<=cx+2;chunkX++)for(int chunkZ=cz-2;chunkZ<=cz+2;chunkZ++) {
            if(!level.hasChunk(chunkX,chunkZ))continue;
            for(BlockEntity entity:level.getChunk(chunkX,chunkZ).getBlockEntities().values()) {
                if(!(entity instanceof MirrorBlockEntity))continue;
                BlockPos pos=entity.getBlockPos();
                for(MirrorSurface face:MirrorSurface.faces(pos.getX(),pos.getY(),pos.getZ()))surfaces.add(face);
            }
        }
        return surfaces;
    }

    @Override public int faceMode(MirrorSurface face) {
        Level level=Minecraft.getInstance().level;
        BlockState state=level.getBlockState(query.set(face.x,face.y,face.z));
        if(!(state.getBlock() instanceof MirrorBlock))return MirrorEngine.MODE_NONE;
        if(level.getBlockState(query.set(face.x+face.nx,face.y+face.ny,face.z+face.nz)).getBlock() instanceof MirrorBlock)return MirrorEngine.MODE_NONE;
        return MirrorBlock.renderMode(state);
    }

    @Override public void capture(MirrorEngine.Capture<TextureTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        TextureTarget into=capture.into;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=matrix(self.view),projection=matrix(self.projection),perspective=matrix(capture.perspective);
        Camera parentCamera=(Camera)parent.camera;
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,delta.getGameTimeDeltaPartialTick(false));
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parentCamera.getYRot():surface.nz!=0?180-parentCamera.getYRot():-parentCamera.getYRot(),surface.ny!=0?-parentCamera.getXRot():parentCamera.getXRot());
        self.camera=virtual;
        RenderTarget parentTarget=(RenderTarget)parent.target;
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull;
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        redirect=into;
        try {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),capture.playersOnly?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
            boolean shaderPass=capture.shaders&&!capture.playersOnly;
            if(shaderPass)ShaderReflection.begin(capture.perspective,self.view,surface.nx,surface.ny,surface.nz,capture.offsetX(),capture.offsetY(),capture.offsetZ(),virtual,parent.view,parent.eye,self.eye,capture.prior);
            try {
                RenderSystem.setProjectionMatrix(projection,ProjectionType.PERSPECTIVE);
                glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
                if(capture.playersOnly) {
                    renderPlayersOnEndSky(virtual,eye,view);
                } else {
                    // Vanilla camera-cube fitting can loop forever with an oblique near plane.
                    // Cull conservatively with perspective; retain oblique projection for drawing.
                    mc.levelRenderer.prepareCullFrustum(eye,view,perspective);
                    mc.levelRenderer.renderLevel(allocator,delta,false,virtual,game,view,projection);
                }
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            if(capture.nested!=null)capture.nested.run();
        } finally {
            mc.smartCull=oldSmartCull;
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:parentTarget;
            RenderSystem.setProjectionMatrix(matrix(parent.projection),ProjectionType.PERSPECTIVE);
            mc.levelRenderer.prepareCullFrustum(new Vec3(parent.eye[0],parent.eye[1],parent.eye[2]),matrix(parent.view),perspective);
            mc.getEntityRenderDispatcher().prepare(mc.level,parentCamera,mc.crosshairPickEntity);
        }
    }

    @Override public void composite(TextureTarget into,TextureTarget source,float[] viewProjection,double[] eye,float[] sourceViewProjection,double[] sourceEye,List<MirrorSurface> faces) {
        RenderTarget target=into==null?Minecraft.getInstance().getMainRenderTarget():into;
        int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFbo==0)compositeFbo=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFbo);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,glId(target),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,((GlTexture)GpuTextures.unwrap(target.getDepthTexture())).glId(),0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mirror composite framebuffer incomplete");
            glViewport(0,0,target.width,target.height);
            COMPOSITE.drawMergedBatch(glId(source),viewProjection,eye[0],eye[1],eye[2],sourceViewProjection,sourceEye[0],sourceEye[1],sourceEye[2],faces);
        } finally { glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw); }
    }

    /** VRChat-style pass: skips world rendering entirely, so it costs a sky draw plus the player models. */
    private void renderPlayersOnEndSky(Camera camera,Vec3 eye,Matrix4f view) {
        Minecraft mc=Minecraft.getInstance();
        EntityRenderDispatcher dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.level,camera,mc.crosshairPickEntity);
        Matrix4fStack modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(view);
        // Shadows are projected onto world blocks, which are not drawn here.
        dispatcher.setRenderShadow(false);
        FogParameters oldFog=RenderSystem.getShaderFog();
        try {
            RenderSystem.setShaderFog(FogParameters.NO_FOG);
            ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$skyRenderer().renderEndSky();
            Lighting.setupLevel();
            PoseStack pose=new PoseStack();
            MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
            for(AbstractClientPlayer player:mc.level.players()) {
                if(!player.shouldRender(eye.x,eye.y,eye.z))continue;
                float partial=delta.getGameTimeDeltaPartialTick(!mc.level.tickRateManager().isEntityFrozen(player));
                dispatcher.render(player,Mth.lerp(partial,player.xOld,player.getX())-eye.x,Mth.lerp(partial,player.yOld,player.getY())-eye.y,Mth.lerp(partial,player.zOld,player.getZ())-eye.z,partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
            }
            buffers.endBatch();
        } finally {
            RenderSystem.setShaderFog(oldFog);
            dispatcher.setRenderShadow(true);
            modelView.popMatrix();
        }
    }

    public static void release() {
        ENGINE.release();
        if(compositeFbo!=0)glDeleteFramebuffers(compositeFbo);
        compositeFbo=0;
        COMPOSITE.close();
        redirect=null;
    }

    private static int glId(RenderTarget target) { return ((GlTexture)GpuTextures.unwrap(target.getColorTexture())).glId(); }
    private static float[] array(Matrix4f matrix) { return matrix.get(new float[16]); }
    private static Matrix4f matrix(float[] values) { return new Matrix4f().set(values); }
}
