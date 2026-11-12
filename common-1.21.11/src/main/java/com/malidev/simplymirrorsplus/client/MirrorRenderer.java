package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.malidev.simplymirrorsplus.block.MirrorBlock;
import com.malidev.simplymirrorsplus.block.MirrorBlockEntity;
import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL33.*;

/** Minecraft 1.21.11 side of the mirror pipeline; {@link MirrorEngine} decides what to render. */
public final class MirrorRenderer implements MirrorEngine.Platform<TextureTarget> {
    public static RenderTarget redirect;
    /** Set while a Players Only reflection renders; the level state is trimmed once extracted. */
    private static boolean playersPass;
    private static final MirrorRenderer PLATFORM=new MirrorRenderer();
    private static final MirrorEngine<TextureTarget> ENGINE=new MirrorEngine<>(PLATFORM);
    private static final GlMirrorComposite COMPOSITE=new GlMirrorComposite();
    private static PerspectiveProjectionMatrixBuffer projectionBuffer;
    private static GlobalSettingsUniform globalSettings;
    private static int compositeFbo;
    private final BlockPos.MutableBlockPos query=new BlockPos.MutableBlockPos();
    private GraphicsResourceAllocator allocator;
    private DeltaTracker delta;
    private GpuBufferSlice fog;
    private Vector4f fogColor;
    private boolean sky;

    private MirrorRenderer() { }

    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,Matrix4f mainView,Matrix4f mainProjection,GpuBufferSlice fog,Vector4f fogColor,boolean sky) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        if(!(GpuTextures.unwrap(main.getColorTexture()) instanceof GlTexture))return;
        PLATFORM.allocator=allocator;
        PLATFORM.delta=delta;
        PLATFORM.fog=fog;
        PLATFORM.fogColor=fogColor;
        PLATFORM.sky=sky;
        Vec3 eye=camera.position();
        MirrorEngine.Pass root=new MirrorEngine.Pass(camera,new double[]{eye.x,eye.y,eye.z},array(mainView),array(mainProjection),0);
        root.target=main;
        CameraRenderState cameraState=state().cameraRenderState;
        Vec3 mainPos=cameraState.pos;
        BlockPos mainBlock=cameraState.blockPos;
        Quaternionf mainOrientation=new Quaternionf(cameraState.orientation);
        var visible=mc.levelRenderer.getVisibleSections();
        var savedVisible=new ArrayList<>(visible);
        GpuBufferSlice savedProjection=RenderSystem.getProjectionMatrixBuffer();
        ProjectionType savedType=RenderSystem.getProjectionType();
        GpuBuffer savedGlobals=RenderSystem.getGlobalSettingsUniform();
        try {
            if(projectionBuffer==null)projectionBuffer=new PerspectiveProjectionMatrixBuffer("Simply Mirrors Plus projection");
            if(globalSettings==null)globalSettings=new GlobalSettingsUniform();
            ENGINE.render(root,main.width,main.height,mc.level,System.nanoTime());
        } finally {
            redirect=null;
            playersPass=false;
            RenderSystem.setProjectionMatrix(savedProjection,savedType);
            RenderSystem.setGlobalSettingsUniform(savedGlobals);
            cameraState.pos=mainPos;cameraState.blockPos=mainBlock;cameraState.orientation=mainOrientation;
            // Section culling follows the last camera; the player's view must not inherit a reflection's list.
            visible.clear();visible.addAll(savedVisible);
        }
    }

    @Override public TextureTarget createTarget(int width,int height) {
        TextureTarget target=new TextureTarget("Simply Mirrors Plus reflection",width,height,true);
        // Reprojected and downscaled images do not land on texel centres.
        int bound=glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D,glId(target));
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D,bound);
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
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,partial);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parentCamera.yRot():surface.nz!=0?180-parentCamera.yRot():-parentCamera.yRot(),surface.ny!=0?-parentCamera.xRot():parentCamera.xRot());
        // Sky and fog colours come from the camera's environment probe, which updates on tick.
        virtual.tick();
        self.camera=virtual;
        CameraRenderState cameraState=state().cameraRenderState;
        cameraState.pos=eye;cameraState.blockPos=BlockPos.containing(eye);cameraState.orientation=new Quaternionf(virtual.rotation());
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull;
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        redirect=into;
        playersPass=capture.playersOnly;
        boolean shaderPass=capture.shaders&&!capture.playersOnly;
        try {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),capture.playersOnly?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
            if(shaderPass)ShaderReflection.begin(capture.perspective,self.view,surface.nx,surface.ny,surface.nz,capture.offsetX(),capture.offsetY(),capture.offsetZ(),virtual,parent.view,parent.eye,self.eye,capture.prior);
            try {
                // Terrain shaders read the camera position from the global settings block.
                globalSettings.update(into.width,into.height,mc.options.glintStrength().get(),mc.level.getGameTime(),delta,
                    mc.options.getMenuBackgroundBlurriness(),virtual,mc.options.textureFiltering().get()==TextureFilteringMethod.RGSS);
                RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
                glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
                // Vanilla camera-cube fitting can loop forever with an oblique near plane.
                // Cull conservatively with perspective; retain oblique projection for drawing.
                mc.levelRenderer.renderLevel(allocator,delta,false,virtual,view,projection,perspective,fog,new Vector4f(fogColor),sky||capture.playersOnly);
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            playersPass=false;
            if(capture.nested!=null)capture.nested.run();
        } finally {
            playersPass=false;
            mc.smartCull=oldSmartCull;
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:(RenderTarget)parent.target;
            mc.getEntityRenderDispatcher().prepare(parentCamera,mc.crosshairPickEntity);
            mc.getBlockEntityRenderDispatcher().prepare(parentCamera);
        }
    }

    /** Runs inside renderLevel once the reflection's view is extracted, before anything is drawn. */
    public static void afterExtract(LevelRenderState state) {
        if(redirect==null||!playersPass)return;
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
        state.weatherRenderState.reset();
        state.haveGlowingEntities=false;
        state.skyRenderState.reset();
        state.skyRenderState.skybox=DimensionType.Skybox.END;
        Minecraft.getInstance().levelRenderer.getVisibleSections().clear();
    }

    /** Particles, clouds and weather are part of the world a Players Only reflection leaves out. */
    public static boolean skipWorldEffects() { return redirect!=null&&playersPass; }

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

    public static void release() {
        ENGINE.release();
        if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        if(globalSettings!=null)globalSettings.close();
        globalSettings=null;
        if(compositeFbo!=0)glDeleteFramebuffers(compositeFbo);
        compositeFbo=0;
        COMPOSITE.close();
        redirect=null;
    }

    private static LevelRenderState state() { return ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).simplymirrorsplus$state(); }
    private static int glId(RenderTarget target) { return ((GlTexture)GpuTextures.unwrap(target.getColorTexture())).glId(); }
    private static float[] array(Matrix4f matrix) { return matrix.get(new float[16]); }
    private static Matrix4f matrix(float[] values) { return new Matrix4f().set(values); }
}
