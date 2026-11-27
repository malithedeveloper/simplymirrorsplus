//? if <1.15 {
/*package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.client.mixin.CameraAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LegacyGameRendererAccessor;
import com.malidev.simplymirrorsplus.client.mixin.LevelRendererAccessor;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import static org.lwjgl.opengl.GL33.*;

/^*
 * Draws reflections on Minecraft 1.14. There the world is drawn by GameRenderer.render with the fixed-function
 * matrix stacks and one shared camera object, so a reflection runs that method again and swaps the camera,
 * both matrices and the viewport from inside it.
 ^/
public final class LegacyRenderer extends MirrorRenderer {
    /^* The view a reflection is being drawn from, or null while the player's own view is drawn. ^/
    private static final class View {
        final double[] eye;
        final float[] view,projection;
        final float yaw,pitch;
        View(double[] eye,float[] view,float[] projection,float yaw,float pitch) {
            this.eye=eye;this.view=view;this.projection=projection;this.yaw=yaw;this.pitch=pitch;
        }
    }

    private View active;
    private float partial;
    private long deadline;

    /^*
     * Called in GameRenderer.render once the camera is set up. For the player's view that is the moment the
     * matrices are known, so the reflections are drawn here; inside a reflection it is where the camera is replaced.
     ^/
    public static void afterCamera(GameRenderer game,float partial,long deadline) {
        LegacyRenderer self=(LegacyRenderer)INSTANCE;
        Minecraft mc=Minecraft.getInstance();
        Camera camera=game.getMainCamera();
        if(redirect!=null) { self.applyView(camera);return; }
        if(mc.level==null||mc.player==null)return;
        RenderTarget main=mc.getMainRenderTarget();
        self.partial=partial;
        self.deadline=deadline;
        Vec3 eye=camera.getPosition();
        MirrorEngine.Pass root=new MirrorEngine.Pass(new float[]{camera.getYRot(),camera.getXRot()},new double[]{eye.x,eye.y,eye.z},
            matrix(GL_MODELVIEW_MATRIX),matrix(GL_PROJECTION_MATRIX),0);
        root.target=main;
        try {
            self.engine.capture(root,main.width,main.height,mc.level,System.nanoTime());
        } finally {
            redirect=null;
            self.active=null;
            main.bindWrite(true);
            // The reflections used the same camera object and matrix stacks; set both up again for the player.
            ((LegacyGameRendererAccessor)game).simplymirrorsplus$setupCamera(partial);
            Entity entity=mc.getCameraEntity()==null?mc.player:mc.getCameraEntity();
            camera.setup(mc.level,entity,mc.options.thirdPersonView>0,mc.options.thirdPersonView==2,partial);
        }
    }

    /^* Called in GameRenderer.render when the world is finished and the hand is next. ^/
    public static void beforeHand() {
        if(redirect!=null)return;
        INSTANCE.engine.composite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /^* The projection of the reflection being drawn; GameRenderer builds its own several times per frame. ^/
    public static Matrix4f projection() {
        View view=((LegacyRenderer)INSTANCE).active;
        return redirect==null||view==null?null:matrix(view.projection);
    }

    @Override public void capture(MirrorEngine.Capture<RenderTarget> capture) {
        Minecraft mc=Minecraft.getInstance();
        MirrorEngine.Pass parent=capture.parent,self=capture.self;
        MirrorSurface surface=capture.surface;
        RenderTarget into=capture.into;
        float[] parentRotation=(float[])parent.camera;
        float yaw=surface.ny!=0?parentRotation[0]:surface.nz!=0?180-parentRotation[0]:-parentRotation[0];
        float pitch=surface.ny!=0?-parentRotation[1]:parentRotation[1];
        self.camera=new float[]{yaw,pitch};
        LegacyGameRendererAccessor game=(LegacyGameRendererAccessor)mc.gameRenderer;
        View outer=active;
        int oldFront=glGetInteger(GL_FRONT_FACE);
        boolean oldSmartCull=mc.smartCull,oldDepthMask=glGetBoolean(GL_DEPTH_WRITEMASK),oldHand=game.simplymirrorsplus$renderHand();
        // The virtual camera is behind the wall; portal clipping replaces ordinary wall occlusion.
        mc.smartCull=false;
        game.simplymirrorsplus$renderHand(false);
        active=new View(self.eye,self.view,self.projection,yaw,pitch);
        redirect=into;
        try {
            GlStateManager.depthMask(true);
            if(capture.playersOnly)into.setClearColor(0,0,0,1);else into.setClearColor(0.615f,0.718f,0.792f,1);
            into.clear(Minecraft.ON_OSX);
            into.bindWrite(true);
            glFrontFace(GL_CCW);
            if(capture.playersOnly) { renderPlayersOnEndSky(mc);backdrop(into,capture); }
            else game.simplymirrorsplus$render(partial,deadline);
            if(capture.nested!=null)capture.nested.run();
        } finally {
            GlStateManager.depthMask(oldDepthMask);
            mc.smartCull=oldSmartCull;
            game.simplymirrorsplus$renderHand(oldHand);
            glFrontFace(oldFront);
            active=outer;
            redirect=parent.depth==0?null:(RenderTarget)parent.target;
            ((RenderTarget)parent.target).bindWrite(true);
        }
    }

    private void applyView(Camera camera) {
        CameraAccessor accessor=(CameraAccessor)camera;
        accessor.simplymirrorsplus$position(new Vec3(active.eye[0],active.eye[1],active.eye[2]));
        accessor.simplymirrorsplus$rotation(active.yaw,active.pitch);
        // Only a detached camera draws the player it follows.
        accessor.simplymirrorsplus$detached(true);
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.multMatrix(matrix(active.projection));
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.multMatrix(matrix(active.view));
    }

    /^* VRChat-style pass: skips world rendering entirely, so it costs a sky draw plus the player models. ^/
    private void renderPlayersOnEndSky(Minecraft mc) {
        Camera camera=mc.gameRenderer.getMainCamera();
        applyView(camera);
        GlStateManager.disableFog();
        // The sky box is 100 blocks out, past the far plane at short view distances; it writes no depth, so draw it at half size.
        GlStateManager.pushMatrix();
        GlStateManager.scalef(.5f,.5f,.5f);
        ((LevelRendererAccessor)mc.levelRenderer).simplymirrorsplus$renderEndSky();
        GlStateManager.popMatrix();
        EntityRenderDispatcher dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.level,mc.font,camera,mc.crosshairPickEntity,mc.options);
        dispatcher.setPosition(active.eye[0],active.eye[1],active.eye[2]);
        // Shadows are projected onto world blocks, which are not drawn here.
        dispatcher.setRenderShadow(false);
        Lighting.turnOn();
        mc.gameRenderer.turnOnLightLayer();
        try {
            for(Player player:mc.level.players())dispatcher.render(player,partial,false);
        } finally {
            mc.gameRenderer.turnOffLightLayer();
            Lighting.turnOff();
            dispatcher.setRenderShadow(true);
        }
    }

    private static float[] matrix(int which) {
        float[] values=new float[16];
        glGetFloatv(which,values);
        return values;
    }
}
*///?}
