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
import static org.lwjgl.opengl.GL33.*;

/** Minecraft 26.1.2 side of the mirror pipeline, on the extracted-state renderer; {@link MirrorEngine} decides what to render. */
public final class MirrorRenderer implements MirrorEngine.Platform<TextureTarget> {
    public static RenderTarget redirect;
    public static Matrix4fc reflectionProjection;
    private static boolean playersPass;
    public static final Matrix4f mainProjection=new Matrix4f();
    private static final MirrorRenderer PLATFORM=new MirrorRenderer();
    private static final MirrorEngine<TextureTarget> ENGINE=new MirrorEngine<>(PLATFORM);
    private static final GlMirrorComposite COMPOSITE=new GlMirrorComposite();
    private static ProjectionMatrixBuffer projectionBuffer;
    private static int compositeFbo;
    private final BlockPos.MutableBlockPos query=new BlockPos.MutableBlockPos();
    private GraphicsResourceAllocator allocator;
    private DeltaTracker delta;
    private float partial;
    private GpuBufferSlice fog;
    private Vector4f color;
    private boolean sky,sodium,captured;
    private LevelRenderState state;

    private MirrorRenderer() { }

    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,CameraRenderState camera,
            Matrix4fc mainViewMatrix,GpuBufferSlice fog,Vector4f color,boolean sky) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        if(!(GpuTextures.unwrap(main.getColorTexture()) instanceof GlTexture))return;
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Matrix4f savedView=new Matrix4f(mainViewMatrix);
        // Use the exact world projection captured before the GPU upload.
        Matrix4f savedProjection=new Matrix4f(camera.projectionMatrix);
        Matrix4f renderedProjection=new Matrix4f(mainProjection);
        LevelRendererAccessor access=(LevelRendererAccessor)mc.levelRenderer;
        LevelRenderState state=access.simplymirrorsplus$state();
        GpuBufferSlice savedProj=RenderSystem.getProjectionMatrixBuffer(),savedFog=RenderSystem.getShaderFog();
        ProjectionType savedType=RenderSystem.getProjectionType();
        GpuBuffer savedGlobals=RenderSystem.getGlobalSettingsUniform();
        boolean oldSmartCull=mc.smartCull;
        var visible=mc.levelRenderer.getVisibleSections();
        var savedVisible=new ArrayList<>(visible);
        PLATFORM.allocator=allocator;
        PLATFORM.delta=delta;
        PLATFORM.partial=partial;
        PLATFORM.fog=fog;
        PLATFORM.color=color;
        PLATFORM.sky=sky;
        PLATFORM.state=state;
        PLATFORM.sodium=ShaderReflection.terrainRendererPresent();
        PLATFORM.captured=false;
        Vec3 eye=camera.pos;
        MirrorEngine.Pass root=new MirrorEngine.Pass(mc.gameRenderer.getMainCamera(),new double[]{eye.x,eye.y,eye.z},savedView.get(new float[16]),renderedProjection.get(new float[16]),0);
        root.target=main;
        try {
            if(projectionBuffer==null)projectionBuffer=new ProjectionMatrixBuffer("Simply Mirrors Plus projection");
            ENGINE.render(root,main.width,main.height,mc.level,System.nanoTime());
        } finally {
            mc.smartCull=oldSmartCull;reflectionProjection=null;redirect=null;
            RenderSystem.setGlobalSettingsUniform(savedGlobals);RenderSystem.setProjectionMatrix(savedProj,savedType);RenderSystem.setShaderFog(savedFog);
            if(PLATFORM.captured) {
                ShaderReflection.end();
                visible.clear();visible.addAll(savedVisible);
                if(PLATFORM.sodium&&!ShaderReflection.enabled())mc.levelRenderer.update(mc.gameRenderer.getMainCamera());
                state.reset();mc.levelRenderer.extractLevel(delta,mc.gameRenderer.getMainCamera(),partial);
                mc.gameRenderer.getMainCamera().extractRenderState(state.cameraRenderState,partial);
                state.cameraRenderState.viewRotationMatrix.set(savedView);state.cameraRenderState.projectionMatrix.set(savedProjection);
            }
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
    @Override public boolean depthZeroToOne() { return RenderSystem.getDevice().isZZeroToOne(); }

    @Override public List<MirrorSurface> findSurfaces(double x,double y,double z) {
        Level level=Minecraft.getInstance().level;
        List<MirrorSurface> surfaces=new ArrayList<>();
        int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
        for(int chunkX=cx-2;chunkX<=cx+2;chunkX++)for(int chunkZ=cz-2;chunkZ<=cz+2;chunkZ++) {
            if(!level.hasChunk(chunkX,chunkZ))continue;
            for(BlockEntity entity:level.getChunk(chunkX,chunkZ).getBlockEntities().values()) {
                if(!(entity instanceof MirrorBlockEntity))continue;
                BlockPos pos=entity.getBlockPos();
                surfaces.addAll(Arrays.asList(MirrorSurface.faces(pos.getX(),pos.getY(),pos.getZ())));
            }
        }
        return surfaces;
    }

    @Override public int faceMode(MirrorSurface face) {
        Level level=Minecraft.getInstance().level;
        BlockState block=level.getBlockState(query.set(face.x,face.y,face.z));
        if(!(block.getBlock() instanceof MirrorBlock))return MirrorEngine.MODE_NONE;
        if(level.getBlockState(query.set(face.x+face.nx,face.y+face.ny,face.z+face.nz)).getBlock() instanceof MirrorBlock)return MirrorEngine.MODE_NONE;
        return MirrorBlock.renderMode(block);
    }

    @Override public void capture(MirrorEngine.Capture<TextureTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        TextureTarget into=capture.into;
        boolean players=capture.playersOnly;
        captured=true;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=new Matrix4f().set(self.view),projection=new Matrix4f().set(self.projection),perspective=new Matrix4f().set(capture.perspective);
        Camera parentCamera=(Camera)parent.camera;
        Camera virtual=new ReflectionCamera(view);
        virtual.setLevel(mc.level);virtual.setEntity(mc.getCameraEntity());virtual.update(delta);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parentCamera.yRot():surface.nz!=0?180-parentCamera.yRot():-parentCamera.yRot(),surface.ny!=0?-parentCamera.xRot():parentCamera.xRot());
        accessor.simplymirrorsplus$detached(true);
        virtual.tick();
        accessor.simplymirrorsplus$frustum(view,perspective,eye);
        self.camera=virtual;
        var visible=mc.levelRenderer.getVisibleSections();
        if(sodium) {
            // Sodium owns its section lists; updating vanilla visibleSections alone has no effect.
            redirect=into;reflectionProjection=projection;mc.smartCull=false;
            if(!capture.shaders&&!players)mc.levelRenderer.update(virtual);
        }
        visible.clear();
        if(!players) {
            // Use loaded section geometry directly: the main camera's occlusion graph may
            // omit a sealed room on the opposite side of the mirror wall.
            var area=((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$viewArea();
            if(area!=null)for(var section:area.sections)
                if(virtual.getCullFrustum().isVisible(section.getBoundingBox()))visible.add(section);
        }
        state.reset();mc.levelRenderer.extractLevel(delta,virtual,partial);virtual.extractRenderState(state.cameraRenderState,partial);
        state.cameraRenderState.viewRotationMatrix.set(view);state.cameraRenderState.projectionMatrix.set(projection);
        state.weatherRenderState.reset();state.particlesRenderState.reset();
        if(players)keepOnlyPlayersOverEndSky(state);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),players?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
        int oldFront=glGetInteger(GL_FRONT_FACE);
        var modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        boolean shaderPass=capture.shaders&&!players;
        try(GpuBuffer globals=globals(eye,into.width,into.height,mc,partial)) {
            redirect=into;
            playersPass=players;
            if(shaderPass)ShaderReflection.begin(capture.perspective,self.view,surface.nx,surface.ny,surface.nz,capture.offsetX(),capture.offsetY(),capture.offsetZ(),virtual,parent.view,parent.eye,self.eye,capture.prior);
            RenderSystem.setGlobalSettingsUniform(globals);
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
            glFrontFace(self.depth%2==1?GL_CW:GL_CCW);
            mc.levelRenderer.renderLevel(allocator,delta,false,state.cameraRenderState,view,fog,new Vector4f(color),players||sky,state.chunkSectionsToRender);
            if(capture.nested!=null)capture.nested.run();
        } finally {
            playersPass=false;
            if(shaderPass)ShaderReflection.end();
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:(RenderTarget)parent.target;
            modelView.popMatrix();
        }
    }

    /** Sodium draws terrain from its own lists, so a Players Only reflection has to stop it separately. */
    public static boolean playersOnlyPass() { return redirect!=null&&playersPass; }

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

    @Override public void composite(TextureTarget into,TextureTarget source,float[] viewProjection,double[] eye,float[] sourceViewProjection,double[] sourceEye,List<MirrorSurface> faces) {
        RenderTarget target=into==null?Minecraft.getInstance().getMainRenderTarget():into;
        int oldDraw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if(compositeFbo==0)compositeFbo=glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,compositeFbo);
        try {
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,((GlTexture)GpuTextures.unwrap(target.getColorTexture())).glId(),0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,((GlTexture)GpuTextures.unwrap(target.getDepthTexture())).glId(),0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Mirror composite framebuffer incomplete");
            glViewport(0,0,target.width,target.height);
            COMPOSITE.drawMergedBatch(glId(source),viewProjection,eye[0],eye[1],eye[2],sourceViewProjection,sourceEye[0],sourceEye[1],sourceEye[2],faces);
        } finally { glBindFramebuffer(GL_DRAW_FRAMEBUFFER,oldDraw); }
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

    private static int glId(TextureTarget target) { return ((GlTexture)GpuTextures.unwrap(target.getColorTexture())).glId(); }

    public static void release() {
        ENGINE.release();
        if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        if(compositeFbo!=0)glDeleteFramebuffers(compositeFbo);
        compositeFbo=0;
        COMPOSITE.close();
        redirect=null;
    }
}
