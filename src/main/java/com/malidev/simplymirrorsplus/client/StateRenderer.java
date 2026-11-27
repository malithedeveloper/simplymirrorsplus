//? if >=1.21.9 <26.1 {
/*package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
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
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import static org.lwjgl.opengl.GL33.*;

/^*
 * Draws reflections on Minecraft 1.21.9 to 1.21.11. renderLevel still takes a camera, but it first extracts
 * the world into a render state, so a Players Only reflection is made by trimming that state.
 ^/
public final class StateRenderer extends MirrorRenderer {
    private PerspectiveProjectionMatrixBuffer projectionBuffer;
    private GlobalSettingsUniform globalSettings;
    private GraphicsResourceAllocator allocator;
    private DeltaTracker delta;
    private GpuBufferSlice fog;
    private Vector4f fogColor;
    private boolean sky;

    /^* Called at the start and the end of LevelRenderer.renderLevel with that method's own arguments: reflections are drawn before the player's view and placed on the mirrors after it. ^/
    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,Matrix4f view,Matrix4f projection,GpuBufferSlice fog,Vector4f fogColor,boolean sky,boolean before) {
        if(redirect!=null)return;
        // A shader pack finishes its frame at the end of renderLevel, so with one active the reflections stay there.
        boolean capture=before!=ShaderReflection.enabled();
        if(before&&!capture)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        if(!openGl(main))return;
        StateRenderer self=(StateRenderer)INSTANCE;
        self.allocator=allocator;
        self.delta=delta;
        self.fog=fog;
        self.fogColor=fogColor;
        self.sky=sky;
        Vec3 eye=camera.position();
        MirrorEngine.Pass root=new MirrorEngine.Pass(camera,new double[]{eye.x,eye.y,eye.z},array(view),array(projection),0);
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
            if(self.projectionBuffer==null)self.projectionBuffer=new PerspectiveProjectionMatrixBuffer("Simply Mirrors Plus projection");
            if(self.globalSettings==null)self.globalSettings=new GlobalSettingsUniform();
            if(capture)self.engine.capture(root,main.width,main.height,mc.level,System.nanoTime());
            if(!before)self.engine.composite();
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

    @Override void releaseResources() {
        super.releaseResources();
        if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        if(globalSettings!=null)globalSettings.close();
        globalSettings=null;
    }

    @Override public void capture(MirrorEngine.Capture<RenderTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        RenderTarget into=capture.into;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=matrix(self.view),projection=matrix(self.projection),perspective=matrix(capture.perspective);
        Camera parentCamera=(Camera)parent.camera;
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,partial);
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        //? if >=1.21.11 {
        /^float yaw=parentCamera.yRot(),pitch=parentCamera.xRot();
        ^///?} else {
        float yaw=parentCamera.getYRot(),pitch=parentCamera.getXRot();
        //?}
        accessor.simplymirrorsplus$rotation(surface.ny!=0?yaw:surface.nz!=0?180-yaw:-yaw,surface.ny!=0?-pitch:pitch);
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
            if(shaderPass)ShaderReflection.begin(capture,virtual);
            try {
                // Terrain shaders read the camera position from the global settings block.
                //? if >=1.21.11 {
                /^globalSettings.update(into.width,into.height,mc.options.glintStrength().get(),mc.level.getGameTime(),delta,
                    mc.options.getMenuBackgroundBlurriness(),virtual,mc.options.textureFiltering().get()==net.minecraft.client.TextureFilteringMethod.RGSS);
                ^///?} else {
                globalSettings.update(into.width,into.height,mc.options.glintStrength().get(),mc.level.getGameTime(),delta,mc.options.getMenuBackgroundBlurriness());
                //?}
                RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
                glFrontFace(GL_CCW);
                // Vanilla camera-cube fitting can loop forever with an oblique near plane.
                // Cull conservatively with perspective; retain oblique projection for drawing.
                mc.levelRenderer.renderLevel(allocator,delta,false,virtual,view,projection,perspective,fog,new Vector4f(fogColor),sky||capture.playersOnly);
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            playersPass=false;
            if(capture.playersOnly)backdrop(into,capture);
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

    /^* Runs inside renderLevel once the reflection's view is extracted, before anything is drawn. ^/
    public static void afterExtract(LevelRenderState state) {
        if(!playersOnlyPass())return;
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
        //? if >=1.21.11 {
        /^state.skyRenderState.skybox=DimensionType.Skybox.END;
        ^///?} else {
        state.skyRenderState.skyType=net.minecraft.client.renderer.DimensionSpecialEffects.SkyType.END;
        //?}
        Minecraft.getInstance().levelRenderer.getVisibleSections().clear();
    }

    private static LevelRenderState state() { return ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).simplymirrorsplus$state(); }
}
*///?}
