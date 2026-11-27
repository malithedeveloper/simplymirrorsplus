//? if >=1.15 <1.21.9 {
package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
//? if >=1.19.3 {
import org.joml.Matrix4f;
//?} else {
/*import com.mojang.math.Matrix4f;
*///?}
//? if >=1.21
import net.minecraft.client.DeltaTracker;
//? if >=1.21.2 {
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
*///?}
//? if >=1.21.6 {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Vector4f;
*///?}
import static org.lwjgl.opengl.GL33.*;

/** Draws reflections on Minecraft up to 1.21.8, where LevelRenderer.renderLevel takes a camera and draws at once. */
public final class DirectRenderer extends MirrorRenderer {
    //? if >=1.21 {
    private DeltaTracker delta;
    //?} else {
    /*private float partial;
    private long deadline;
    *///?}
    //? if >=1.21.2 {
    private GraphicsResourceAllocator allocator;
    //?} else {
    /*private LightTexture light;
    *///?}
    //? if <1.21.6 {
    private GameRenderer game;
    //?} else {
    /*private GpuBufferSlice fog;
    private Vector4f fogColor;
    private boolean sky;
    private net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer projectionBuffer;
    *///?}

    /** Called at the start and the end of LevelRenderer.renderLevel with that method's own arguments: reflections are drawn before the player's view and placed on the mirrors after it. */
    //? if >=1.21.6 {
    /*public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,Matrix4f view,Matrix4f projection,GpuBufferSlice fog,Vector4f fogColor,boolean sky,boolean before) {
    *///?} else if >=1.21.2 {
    public static void render(GraphicsResourceAllocator allocator,DeltaTracker delta,Camera camera,GameRenderer game,Matrix4f view,Matrix4f projection,boolean before) {
    //?} else if >=1.21 {
    /*public static void render(DeltaTracker delta,Camera camera,GameRenderer game,LightTexture light,Matrix4f view,Matrix4f projection,boolean before) {
    *///?} else if >=1.20.5 {
    /*public static void render(float partial,long deadline,Camera camera,GameRenderer game,LightTexture light,Matrix4f view,Matrix4f projection,boolean before) {
    *///?} else {
    /*public static void render(PoseStack pose,float partial,long deadline,Camera camera,GameRenderer game,LightTexture light,Matrix4f projection,boolean before) {
        Matrix4f view=pose.last().pose();
    *///?}
        if(redirect!=null)return;
        // A shader pack finishes its frame at the end of renderLevel, so with one active the reflections stay there.
        boolean capture=before!=ShaderReflection.enabled();
        if(before&&!capture)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        //? if >=1.21.5
        //if(!openGl(main))return;
        DirectRenderer self=(DirectRenderer)INSTANCE;
        //? if >=1.21 {
        self.delta=delta;
        //?} else {
        /*self.partial=partial;
        self.deadline=deadline;
        *///?}
        //? if >=1.21.2 {
        self.allocator=allocator;
        //?} else {
        /*self.light=light;
        *///?}
        //? if <1.21.6 {
        self.game=game;
        //?} else {
        /*self.fog=fog;
        self.fogColor=fogColor;
        self.sky=sky;
        GpuBufferSlice savedProjection=RenderSystem.getProjectionMatrixBuffer();
        ProjectionType savedType=RenderSystem.getProjectionType();
        if(self.projectionBuffer==null)self.projectionBuffer=new net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer("Simply Mirrors Plus projection");
        *///?}
        //? if >=1.16 <1.21 {
        /*// The last Fabulous post pass overwrites the main depth buffer, which would reject every mirror fragment.
        if(!before&&((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$transparencyChain()!=null) {
            main.copyDepthFrom(mc.levelRenderer.getTranslucentTarget());
            main.bindWrite(true);
        }
        *///?}
        Vec3 eye=camera.getPosition();
        MirrorEngine.Pass root=new MirrorEngine.Pass(camera,new double[]{eye.x,eye.y,eye.z},array(view),array(projection),0);
        root.target=main;
        // A reflection replaces the list of terrain to draw; the player's view gets its own back.
        @SuppressWarnings("unchecked")
        java.util.List<Object> visible=(java.util.List<Object>)(Object)((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$visible();
        java.util.List<Object> savedVisible=capture?new java.util.ArrayList<Object>(visible):null;
        //? if >=1.18 {
        // The game's own frustum for this frame is kept and put back as it is. It must not be rebuilt from the
        // projection the frame is drawn with: right after joining a world that one has a field of view of zero,
        // and a frustum made from it sends the game's terrain search into an endless loop.
        net.minecraft.client.renderer.culling.Frustum savedFrustum=capture?((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$cullingFrustum():null;
        //?}
        try {
            if(capture)self.engine.capture(root,main.width,main.height,mc.level,System.nanoTime());
            if(!before)self.engine.composite();
        } finally {
            redirect=null;
            if(savedVisible!=null) { visible.clear();visible.addAll(savedVisible); }
            //? if >=1.18 {
            if(savedFrustum!=null)((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$setCullingFrustum(savedFrustum);
            //?}
            //? if <1.21.5
            main.bindWrite(true);
            //? if >=1.21.6 {
            /*RenderSystem.setProjectionMatrix(savedProjection,savedType);
            *///?} else {
            self.setProjection(projection);
            //?}
        }
    }

    @Override void releaseResources() {
        super.releaseResources();
        //? if <1.20.2
        //ReflectionSections.clear();
        //? if >=1.21.6 {
        /*if(projectionBuffer!=null)projectionBuffer.close();
        projectionBuffer=null;
        *///?}
    }

    @Override public void capture(MirrorEngine.Capture<RenderTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        RenderTarget into=capture.into;
        Vec3 eye=new Vec3(self.eye[0],self.eye[1],self.eye[2]);
        Matrix4f view=matrix(self.view),projection=matrix(self.projection),perspective=matrix(capture.perspective);
        Camera parentCamera=(Camera)parent.camera;
        Camera virtual=new Camera();
        virtual.setup(mc.level,mc.getCameraEntity(),true,false,partialTick());
        CameraAccessor accessor=(CameraAccessor)(Object)virtual;
        accessor.simplymirrorsplus$position(eye);
        accessor.simplymirrorsplus$rotation(surface.ny!=0?parentCamera.getYRot():surface.nz!=0?180-parentCamera.getYRot():-parentCamera.getYRot(),surface.ny!=0?-parentCamera.getXRot():parentCamera.getXRot());
        self.camera=virtual;
        RenderTarget parentTarget=(RenderTarget)parent.target;
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull;
        //? if <1.21.5
        boolean oldDepthMask=glGetBoolean(GL_DEPTH_WRITEMASK);
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        redirect=into;
        try {
            clear(into,capture.playersOnly);
            boolean shaderPass=capture.shaders&&!capture.playersOnly;
            //? if >=1.21 <1.21.5 {
            if(shaderPass&&DepthTextureParity.match(mc.getMainRenderTarget().getDepthTextureId(),into.getDepthTextureId(),into.width,into.height))
                java.util.logging.Logger.getLogger("simplymirrorsplus").info("Matched mirror depth internal format to Iris main target");
            //?}
            if(shaderPass)ShaderReflection.begin(capture,virtual);
            try {
                setProjection(projection);
                glFrontFace(GL_CCW);
                if(capture.playersOnly) { renderPlayersOnEndSky(virtual,eye,view);backdrop(into,capture); }
                else renderWorld(virtual,eye,view,projection,perspective);
            } finally {
                if(shaderPass)ShaderReflection.end();
            }
            if(capture.nested!=null)capture.nested.run();
        } finally {
            //? if <1.21.5
            RenderSystem.depthMask(oldDepthMask);
            mc.smartCull=oldSmartCull;
            glFrontFace(oldFront);
            redirect=parent.depth==0?null:parentTarget;
            //? if <1.21.5
            parentTarget.bindWrite(true);
            //? if <1.21.6
            setProjection(matrix(parent.projection));
            // A reflection that holds this one gets its own frustum back; the player's is put back as a whole when all are drawn.
            if(parent.depth>0)cullFrustum(new Vec3(parent.eye[0],parent.eye[1],parent.eye[2]),matrix(parent.view),matrix(parent.cull));
            mc.getEntityRenderDispatcher().prepare(mc.level,parentCamera,mc.crosshairPickEntity);
        }
    }

    private void clear(RenderTarget into,boolean black) {
        //? if >=1.21.5 {
        /*RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(into.getColorTexture(),black?0xff000000:0xff9db7ca,into.getDepthTexture(),1.0);
        *///?} else {
        // Iris final compositing leaves depth writes disabled; clears obey that mask.
        RenderSystem.depthMask(true);
        if(black)into.setClearColor(0,0,0,1);else into.setClearColor(0.615f,0.718f,0.792f,1);
        //? if >=1.21.2 {
        into.clear();
        //?} else {
        /*into.clear(Minecraft.ON_OSX);
        *///?}
        into.bindWrite(true);
        //?}
    }

    private void setProjection(Matrix4f projection) {
        //? if >=1.21.6 {
        /*RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),ProjectionType.PERSPECTIVE);
        *///?} else if >=1.21.2 {
        RenderSystem.setProjectionMatrix(projection,ProjectionType.PERSPECTIVE);
        //?} else {
        /*game.resetProjectionMatrix(projection);
        *///?}
    }

    private float partialTick() {
        //? if >=1.21 {
        return delta.getGameTimeDeltaPartialTick(false);
        //?} else {
        /*return partial;
        *///?}
    }

    /**
     * Vanilla fits the frustum around the camera's block, which can loop forever with an oblique near plane.
     * Culling with the plain perspective is conservative; the oblique projection is still what draws.
     */
    private void cullFrustum(Vec3 eye,Matrix4f view,Matrix4f perspective) {
        //? if >=1.20.5 {
        Minecraft.getInstance().levelRenderer.prepareCullFrustum(eye,view,perspective);
        //?} else if >=1.18 {
        /*Minecraft.getInstance().levelRenderer.prepareCullFrustum(poseOf(view),eye,perspective);
        *///?}
    }

    private void renderWorld(Camera virtual,Vec3 eye,Matrix4f view,Matrix4f projection,Matrix4f perspective) {
        Minecraft mc=Minecraft.getInstance();
        cullFrustum(eye,view,perspective);
        //? if >=1.21.6 {
        /*mc.levelRenderer.renderLevel(allocator,delta,false,virtual,view,projection,fog,new Vector4f(fogColor),sky);
        *///?} else if >=1.21.4 {
        mc.levelRenderer.renderLevel(allocator,delta,false,virtual,game,view,projection);
        //?} else if >=1.21.2 {
        /*mc.levelRenderer.renderLevel(allocator,delta,false,virtual,game,game.lightTexture(),view,projection);
        *///?} else if <1.16 {
        /*mc.levelRenderer.renderLevel(poseOf(view),partial,deadline,false,virtual,game,light,projection);
        *///?} else {
        /*// Fabulous post passes read fixed window-sized targets, so reflections use the Fancy path.
        LevelRendererAccessor access=(LevelRendererAccessor)mc.levelRenderer;
        net.minecraft.client.renderer.PostChain transparency=access.simplymirrorsplus$transparencyChain();
        access.simplymirrorsplus$setTransparencyChain(null);
        try {
            //? if >=1.21 {
            mc.levelRenderer.renderLevel(delta,false,virtual,game,light,view,projection);
            //?} else if >=1.20.5 {
            /^mc.levelRenderer.renderLevel(partial,deadline,false,virtual,game,light,view,projection);
            ^///?} else {
            /^mc.levelRenderer.renderLevel(poseOf(view),partial,deadline,false,virtual,game,light,projection);
            ^///?}
        } finally {
            access.simplymirrorsplus$setTransparencyChain(transparency);
        }
        *///?}
    }

    /** VRChat-style pass: skips world rendering entirely, so it costs a sky draw plus the player models. */
    private void renderPlayersOnEndSky(Camera camera,Vec3 eye,Matrix4f view) {
        Minecraft mc=Minecraft.getInstance();
        EntityRenderDispatcher dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.level,camera,mc.crosshairPickEntity);
        //? if >=1.20.5 {
        // From 1.20.5 the view sits on the global model-view stack and entities get an identity pose.
        //? if <1.21.2 {
        /*net.minecraft.client.renderer.FogRenderer.setupNoFog();
        ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$renderEndSky(poseOf(view));
        *///?}
        org.joml.Matrix4fStack modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(view);
        //? if <1.21.2
        //RenderSystem.applyModelViewMatrix();
        PoseStack pose=new PoseStack();
        //? if >=1.21.5 <1.21.6
        //net.minecraft.client.renderer.FogParameters oldFog=RenderSystem.getShaderFog();
        //?} else {
        /*net.minecraft.client.renderer.FogRenderer.setupNoFog();
        PoseStack pose=poseOf(view);
        ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$renderEndSky(pose);
        *///?}
        // Shadows are projected onto world blocks, which are not drawn here.
        dispatcher.setRenderShadow(false);
        try {
            //? if >=1.21.2 {
            //? if <1.21.6
            RenderSystem.setShaderFog(net.minecraft.client.renderer.FogParameters.NO_FOG);
            //? if >=1.21.4 {
            ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$skyRenderer().renderEndSky();
            //?} else {
            /*((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$skyRenderer().renderEndSky(pose);
            *///?}
            //?}
            //? if >=1.21.6 {
            /*mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
            *///?} else if >=1.20.5 {
            Lighting.setupLevel();
            //?} else {
            /*Lighting.setupLevel(view);
            *///?}
            MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
            for(AbstractClientPlayer player:mc.level.players()) {
                if(!player.shouldRender(eye.x,eye.y,eye.z))continue;
                //? if >=1.21 {
                float partial=delta.getGameTimeDeltaPartialTick(!mc.level.tickRateManager().isEntityFrozen(player));
                //?}
                double x=Mth.lerp(partial,player.xOld,player.getX())-eye.x,y=Mth.lerp(partial,player.yOld,player.getY())-eye.y,z=Mth.lerp(partial,player.zOld,player.getZ())-eye.z;
                //? if >=1.21.2 {
                dispatcher.render(player,x,y,z,partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
                //?} else if >=1.17 {
                /*dispatcher.render(player,x,y,z,Mth.lerp(partial,player.yRotO,player.getYRot()),partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
                *///?} else {
                /*dispatcher.render(player,x,y,z,Mth.lerp(partial,player.yRotO,player.yRot),partial,pose,buffers,dispatcher.getPackedLightCoords(player,partial));
                *///?}
            }
            buffers.endBatch();
        } finally {
            dispatcher.setRenderShadow(true);
            //? if >=1.20.5 {
            modelView.popMatrix();
            //? if <1.21.2
            //RenderSystem.applyModelViewMatrix();
            //? if >=1.21.5 <1.21.6
            //RenderSystem.setShaderFog(oldFog);
            //?}
        }
    }

    //? if <1.21.2 {
    /*private static PoseStack poseOf(Matrix4f view) {
        PoseStack pose=new PoseStack();
        //? if >=1.19.3 {
        pose.last().pose().set(view);
        pose.last().normal().set(new org.joml.Matrix3f(view));
        //?} else {
        /^pose.last().pose().multiply(view);
        pose.last().normal().load(new com.mojang.math.Matrix3f(view));
        ^///?}
        return pose;
    }
    *///?}
}
//?}
