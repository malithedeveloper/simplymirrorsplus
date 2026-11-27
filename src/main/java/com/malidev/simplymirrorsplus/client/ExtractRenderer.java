//? if >=26.1 {
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
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import static org.lwjgl.opengl.GL33.*;

/^*
 * Draws reflections on Minecraft 26.1 and later. The game extracts the world into a render state before
 * renderLevel runs, so each reflection extracts its own view into that state and the player's view is
 * extracted again afterwards.
 ^/
public final class ExtractRenderer extends MirrorRenderer {
    /^* The reflection's projection while it renders; Sodium asks the game for it instead of using renderLevel's. ^/
    public static Matrix4fc reflectionProjection;
    /^* The world projection of the current frame, with view bobbing and nausea, captured before its upload. ^/
    public static final Matrix4f mainProjection=new Matrix4f();
    private ProjectionMatrixBuffer projectionBuffer;
    //? if >=26.2 {
    /^private GlobalSettingsUniform globalSettings;
    private final CameraRenderState reflectionCameraState=new CameraRenderState();
    ^///?}
    private GraphicsResourceAllocator allocator;
    private DeltaTracker delta;
    private float partial;
    private GpuBufferSlice fog;
    private Vector4f color;
    private boolean sky,sodium,captured;
    private LevelRenderState state;

    /^* Called at the end of LevelRenderer.renderLevel with that method's own arguments. ^/
    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,CameraRenderState camera,
            Matrix4fc mainViewMatrix,GpuBufferSlice fog,Vector4f color,boolean sky) {
        if(redirect!=null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mainTarget();
        if(!openGl(main))return;
        ExtractRenderer self=(ExtractRenderer)INSTANCE;
        float partial=delta.getGameTimeDeltaPartialTick(false);
        Matrix4f savedView=new Matrix4f(mainViewMatrix);
        // Use the exact world projection captured before the GPU upload.
        Matrix4f renderedProjection=new Matrix4f(mainProjection);
        LevelRenderState state=((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$state();
        //? if >=26.2 {
        /^CameraRenderState mainCameraState=state.cameraRenderState;
        long gameTime=state.gameTime;
        ^///?} else {
        Matrix4f savedProjection=new Matrix4f(camera.projectionMatrix);
        //?}
        GpuBufferSlice savedProj=RenderSystem.getProjectionMatrixBuffer(),savedFog=RenderSystem.getShaderFog();
        ProjectionType savedType=RenderSystem.getProjectionType();
        GpuBuffer savedGlobals=RenderSystem.getGlobalSettingsUniform();
        boolean oldSmartCull=mc.smartCull;
        var visible=visibleSections();
        var savedVisible=new ArrayList<>(visible);
        self.allocator=allocator;
        self.delta=delta;
        self.partial=partial;
        self.fog=fog;
        self.color=color;
        self.sky=sky;
        self.state=state;
        self.sodium=ShaderReflection.terrainRendererPresent();
        self.captured=false;
        Vec3 eye=camera.pos;
        MirrorEngine.Pass root=new MirrorEngine.Pass(mainCamera(),new double[]{eye.x,eye.y,eye.z},array(savedView),array(renderedProjection),0);
        root.target=main;
        try {
            if(self.projectionBuffer==null)self.projectionBuffer=new ProjectionMatrixBuffer("Simply Mirrors Plus projection");
            //? if >=26.2
            //if(self.globalSettings==null)self.globalSettings=new GlobalSettingsUniform();
            self.engine.capture(root,main.width,main.height,mc.level,System.nanoTime());
            self.engine.composite();
        } finally {
            mc.smartCull=oldSmartCull;reflectionProjection=null;redirect=null;
            RenderSystem.setGlobalSettingsUniform(savedGlobals);RenderSystem.setProjectionMatrix(savedProj,savedType);RenderSystem.setShaderFog(savedFog);
            if(self.captured) {
                ShaderReflection.end();
                visible.clear();visible.addAll(savedVisible);
                //? if >=26.2 {
                /^state.cameraRenderState=mainCameraState;
                state.gameTime=gameTime;
                state.entityRenderStates.clear();state.blockEntityRenderStates.clear();state.blockBreakingRenderStates.clear();
                // The main view's entities still need their own dispatcher camera for the hand and outline passes.
                mc.levelRenderer.entityRenderDispatcher().prepare(mainCamera(),mc.crosshairPickEntity);
                mc.levelRenderer.blockEntityRenderDispatcher().prepare(mainCameraState.pos);
                ^///?} else {
                if(self.sodium&&!ShaderReflection.enabled())mc.levelRenderer.update(mainCamera());
                state.reset();mc.levelRenderer.extractLevel(delta,mainCamera(),partial);
                mainCamera().extractRenderState(state.cameraRenderState,partial);
                state.cameraRenderState.viewRotationMatrix.set(savedView);state.cameraRenderState.projectionMatrix.set(savedProjection);
                //?}
            }
        }
    }

    @Override void releaseResources() {
        super.releaseResources();
        if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        //? if >=26.2 {
        /^if(globalSettings!=null)globalSettings.close();
        globalSettings=null;
        ^///?}
    }

    //? if >=26.2 {
    /^ExtractRenderer() { composite.reversedDepth=true;backdrop.reversedDepth=true;frameDrawer.reversedDepth=true; }

    @Override public boolean depthZeroToOne() { return RenderSystem.getDevice().getDeviceInfo().isZZeroToOne(); }
    /^* From 26.2 the near plane maps to the top of the depth range. ^/
    @Override public boolean depthReversed() { return true; }
    ^///?} else {
    @Override public boolean depthZeroToOne() { return RenderSystem.getDevice().isZZeroToOne(); }
    //?}

    private static Camera mainCamera() {
        //? if >=26.2 {
        /^return Minecraft.getInstance().gameRenderer.mainCamera();
        ^///?} else {
        return Minecraft.getInstance().gameRenderer.getMainCamera();
        //?}
    }

    private static it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection> visibleSections() {
        //? if >=26.2 {
        /^return Minecraft.getInstance().levelRenderer.visibleSections();
        ^///?} else {
        return Minecraft.getInstance().levelRenderer.getVisibleSections();
        //?}
    }

    @Override public void capture(MirrorEngine.Capture<RenderTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        RenderTarget into=capture.into;
        boolean players=capture.playersOnly;
        captured=true;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=matrix(self.view),projection=matrix(self.projection),perspective=matrix(capture.perspective);
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
        var visible=visibleSections();
        if(sodium) {
            // Sodium owns its section lists; updating vanilla visibleSections alone has no effect.
            redirect=into;reflectionProjection=projection;mc.smartCull=false;
            //? if <26.2
            if(!capture.shaders&&!players)mc.levelRenderer.update(virtual);
        }
        visible.clear();
        if(!players) {
            // Use loaded section geometry directly: the main camera's occlusion graph may
            // omit a sealed room on the opposite side of the mirror wall.
            //? if >=26.2 {
            /^var area=mc.levelRenderer.viewArea();
            if(area!=null)for(var section:((com.malidev.simplymirrorsplus.client.mixin.ViewAreaAccessor)area).simplymirrorsplus$sections())
                if(virtual.getCullFrustum().isVisible(section.getBoundingBox()))visible.add(section);
            ^///?} else {
            var area=((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$viewArea();
            if(area!=null)for(var section:area.sections)
                if(virtual.getCullFrustum().isVisible(section.getBoundingBox()))visible.add(section);
            // Vanilla builds only the sections the player has looked at; a mirror also shows what is behind them.
            var regions=new net.minecraft.client.renderer.chunk.RenderRegionCache();
            for(var section:visible)if(section.isDirty()&&section.hasAllNeighbors()) { section.rebuildSectionAsync(regions);section.setNotDirty(); }
            //?}
        }
        //? if >=26.2 {
        /^extract(mc,virtual);
        state.cameraRenderState.viewRotationMatrix.set(view);state.cameraRenderState.projectionMatrix.set(projection);
        if(players)keepOnlyPlayersOverEndSky(state);
        // Reversed depth clears to 0, the far plane.
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),players?new Vector4f(0,0,0,1):new Vector4f(.616f,.718f,.792f,1),into.getDepthTexture(),0.0);
        ^///?} else {
        // Extraction culls terrain; with the image set, the player's occlusion graph is left alone.
        redirect=into;
        state.reset();mc.levelRenderer.extractLevel(delta,virtual,partial);virtual.extractRenderState(state.cameraRenderState,partial);
        state.cameraRenderState.viewRotationMatrix.set(view);state.cameraRenderState.projectionMatrix.set(projection);
        state.weatherRenderState.reset();state.particlesRenderState.reset();
        if(players)keepOnlyPlayersOverEndSky(state);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),players?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
        //?}
        int oldFront=glGetInteger(GL_FRONT_FACE);
        var modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        boolean shaderPass=capture.shaders&&!players;
        //? if >=26.2 {
        /^try {
            redirect=into;
            playersPass=players;
            if(shaderPass)ShaderReflection.begin(capture,virtual);
            // Terrain shaders read the camera position from the global settings block.
            //? if >=26.3 {
            /^globalSettings.update(into.width,into.height,mc.options.glintStrength().get(),mc.level.getGameTime(),partial,
                mc.options.getMenuBackgroundBlurriness(),eye,mc.options.textureFiltering().get()==net.minecraft.client.TextureFilteringMethod.RGSS);
            ^///?} else {
            globalSettings.update(into.width,into.height,mc.options.glintStrength().get(),mc.level.getGameTime(),delta,
                mc.options.getMenuBackgroundBlurriness(),eye,mc.options.textureFiltering().get()==net.minecraft.client.TextureFilteringMethod.RGSS);
            //?}
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
            glFrontFace(GL_CCW);
            //? if >=26.3 {
            /^mc.levelRenderer.render(allocator,false,state.cameraRenderState,fog,new Vector4f(color),players||sky,false);
            ^///?} else {
            mc.levelRenderer.render(allocator,delta,false,state.cameraRenderState,view,fog,new Vector4f(color),players||sky);
            //?}
            if(players)backdrop(into,capture);
            if(capture.nested!=null)capture.nested.run();
        } finally {
        ^///?} else {
        try(GpuBuffer globals=globals(eye,into.width,into.height,mc,partial)) {
            redirect=into;
            playersPass=players;
            if(shaderPass)ShaderReflection.begin(capture,virtual);
            RenderSystem.setGlobalSettingsUniform(globals);
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
            glFrontFace(GL_CCW);
            mc.levelRenderer.renderLevel(allocator,delta,false,state.cameraRenderState,view,fog,new Vector4f(color),players||sky,state.chunkSectionsToRender);
            if(players)backdrop(into,capture);
            if(capture.nested!=null)capture.nested.run();
        } finally {
        //?}
            playersPass=false;
            if(shaderPass)ShaderReflection.end();
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:(RenderTarget)parent.target;
            modelView.popMatrix();
        }
    }

    //? if >=26.2 {
    /^/^* The parts of LevelExtractor.extract that describe what is visible; section and chunk bookkeeping stays with the main view. ^/
    private void extract(Minecraft mc,Camera virtual) {
        long gameTime=state.gameTime;
        state.reset();
        state.gameTime=gameTime;
        state.playerCompiledSectionCallback=null;
        // Rebuilding the sky renderer here would bind it to the mirror image for good.
        state.shouldResetSkyRenderer=false;
        state.shouldResetChunkLayerSampler=false;
        state.cameraRenderState=reflectionCameraState;
        //? if >=26.3 {
        /^virtual.extractRenderState(reflectionCameraState,delta);
        ^///?} else {
        virtual.extractRenderState(reflectionCameraState,partial);
        //?}
        mc.levelRenderer.blockEntityRenderDispatcher().prepare(virtual.position());
        mc.levelRenderer.entityRenderDispatcher().prepare(virtual,mc.crosshairPickEntity);
        com.malidev.simplymirrorsplus.client.mixin.LevelExtractorInvoker extractor=(com.malidev.simplymirrorsplus.client.mixin.LevelExtractorInvoker)mc.levelExtractor;
        extractor.simplymirrorsplus$entities(virtual,virtual.getCullFrustum(),delta,state);
        extractor.simplymirrorsplus$blockEntities(virtual,partial,state);
        var sky=mc.levelRenderer.skyRenderer();
        if(sky!=null)sky.extractRenderState(mc.level,partial,virtual,state.skyRenderState);
    }
    ^///?}

    /^* Skips the world by editing the extracted state: only player models and the End sky stay. ^/
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
        //? if >=26.2 {
        /^state.shouldShowEntityOutlines=false;
        ^///?} else {
        state.haveGlowingEntities=false;
        //?}
        state.skyRenderState.reset();
        state.skyRenderState.skybox=DimensionType.Skybox.END;
    }

    /^* Terrain draw extraction queries the Camera directly instead of the renderLevel matrix. ^/
    private static final class ReflectionCamera extends Camera {
        private final Matrix4f reflectedView;
        ReflectionCamera(Matrix4f reflectedView) { this.reflectedView=new Matrix4f(reflectedView); }
        @Override public Matrix4f getViewRotationMatrix(Matrix4f destination) { return destination.set(reflectedView); }
    }

    //? if <26.2 {
    private static GpuBuffer globals(Vec3 eye,int width,int height,Minecraft mc,float partial) {
        int x=net.minecraft.util.Mth.floor(eye.x),y=net.minecraft.util.Mth.floor(eye.y),z=net.minecraft.util.Mth.floor(eye.z);
        try(org.lwjgl.system.MemoryStack stack=org.lwjgl.system.MemoryStack.stackPush()) {
            var data=com.mojang.blaze3d.buffers.Std140Builder.onStack(stack,GlobalSettingsUniform.UBO_SIZE).putIVec3(x,y,z)
                .putVec3((float)(x-eye.x),(float)(y-eye.y),(float)(z-eye.z)).putVec2(width,height).putFloat(1)
                .putFloat(((mc.level.getGameTime()%24000)+partial)/24000f).putInt(0).putInt(1).get();
            return RenderSystem.getDevice().createBuffer(()->"Simply Mirrors Plus camera",GpuBuffer.USAGE_UNIFORM,data);
        }
    }
    //?}
}
*///?}
