package com.malidev.simplymirrorsplus.visualtest;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import net.minecraft.world.InteractionHand;
import com.malidev.simplymirrorsplus.visualsupport.VisualCapture;
import com.malidev.simplymirrorsplus.visualsupport.VisualScene;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33.*;
@Mixin(Minecraft.class)
public abstract class VisualProbeMixin {
    @Unique private int simplymirrorsplus$frames;
    @Unique private int simplymirrorsplus$mirrorFrames;
    @Unique private int simplymirrorsplus$scene=-1,simplymirrorsplus$sceneFrames;
    @Unique private long simplymirrorsplus$startPasses;
    @Unique private boolean simplymirrorsplus$captured;
    @Unique private boolean simplymirrorsplus$done;
    @Unique private boolean simplymirrorsplus$stencilReady;
    @Unique private int simplymirrorsplus$motionPhase, simplymirrorsplus$motionStart;
    @Unique private long simplymirrorsplus$motionStartPass;
    @Unique private boolean simplymirrorsplus$joining;
    @Unique private int simplymirrorsplus$cameraPhase;
    @Unique private long simplymirrorsplus$cameraStartPass;
    /**
     * CI's Mesa software renderer can draw fewer than two frames per second,
     * while a Minecraft attack swing lasts only a few 20-Hz game ticks.
     * Preserve the genuine swing's middle pose in this test-only client
     * until several mirror render passes have captured it. This does not
     * bypass the image-difference assertion or affect production code.
     */
    @Inject(method="tick",at=@At("TAIL"))
    private void simplymirrorsplus$holdVisualSwing(CallbackInfo ci) {
        if (!Boolean.getBoolean("simplymirrorsplus.visualCheckMotion")
                || simplymirrorsplus$scene!=0 || simplymirrorsplus$motionPhase!=1) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null) {
            mc.player.attackAnim=0.55f;
            mc.player.oAttackAnim=0.55f;
        }
    }
    @Inject(method="runTick",at=@At("TAIL"))
    private void simplymirrorsplus$probe(CallbackInfo ci) throws Exception {
        if(simplymirrorsplus$done)return;Minecraft mc=Minecraft.getInstance();
        if (Boolean.getBoolean("simplymirrorsplus.visualForceStencil") && !simplymirrorsplus$stencilReady) {
            // The real issue #5 mod calls this NeoForge extension at startup.
            // Use reflection so the shared 1.21.1 QA sources still compile for Fabric.
            Object main = mc.getMainRenderTarget();
            main.getClass().getMethod("enableStencil").invoke(main);
            if (!Boolean.TRUE.equals(main.getClass().getMethod("isStencilEnabled").invoke(main)))
                throw new IllegalStateException("NeoForge main target did not enable stencil");
            simplymirrorsplus$stencilReady = true;
            System.out.println("SIMPLY_MIRRORS_VISUAL_MAIN_STENCIL_ENABLED");
        }
        if(simplymirrorsplus$frames++==0) {mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.bobView().set(false);mc.options.framerateLimit().set(30);mc.options.hideGui=true;}
        if(mc.player==null||mc.level==null) {
            if(simplymirrorsplus$frames%300==0)System.out.println("SIMPLY_MIRRORS_VISUAL_STAGE "+(mc.screen==null?"none":mc.screen.getClass().getName()));
            if(mc.getOverlay()==null&&mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
            if(!simplymirrorsplus$joining&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen) {
                simplymirrorsplus$joining=true;String port=System.getProperty("simplymirrorsplus.visualPort");String address="127.0.0.1:"+port;
                System.out.println("SIMPLY_MIRRORS_VISUAL_CONNECT "+address);ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Mirror test",address,ServerData.Type.OTHER),false,null);
            }
            return;
        }
        mc.player.getAbilities().flying=true;
        int requested=VisualScene.index(mc.gameDirectory.toPath());
        if(requested<0)return;
        if(requested==6){simplymirrorsplus$done=true;mc.stop();return;}
        if(requested!=simplymirrorsplus$scene){simplymirrorsplus$scene=requested;simplymirrorsplus$mirrorFrames=0;simplymirrorsplus$sceneFrames=0;simplymirrorsplus$startPasses=MirrorRenderer.completedPasses;simplymirrorsplus$captured=false;simplymirrorsplus$motionPhase=0;simplymirrorsplus$cameraPhase=0;}
        simplymirrorsplus$sceneFrames++;
        if(MirrorRenderer.completedPasses>simplymirrorsplus$startPasses)simplymirrorsplus$mirrorFrames++;
        int phase=simplymirrorsplus$mirrorFrames/20;
        float yaw=VisualScene.yaw(requested)+(phase==1?20:phase==2?-20:0);
        float base=VisualScene.pitch(requested);
        float pitch=base+(phase==3?(base>0?-20:20):phase==4?(base<0?20:-20):0);
        boolean cameraTest=Boolean.getBoolean("simplymirrorsplus.visualCheckCameraMotion")&&requested==0;
        if(cameraTest&&(simplymirrorsplus$cameraPhase==1||simplymirrorsplus$cameraPhase==2))yaw+=8;
        double[] eye=VisualScene.eye(requested);double px=eye[0],py=eye[1]-1.62,pz=eye[2];
        if(cameraTest&&(simplymirrorsplus$cameraPhase==1||simplymirrorsplus$cameraPhase==2))px+=.35;
        mc.player.setPos(px,py,pz);mc.player.xo=px;mc.player.yo=py;mc.player.zo=pz;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
        if(simplymirrorsplus$captured)return;
        if((MirrorRenderer.completedPasses-simplymirrorsplus$startPasses<120||simplymirrorsplus$mirrorFrames<120)&&simplymirrorsplus$sceneFrames<1800)return;
        if(Boolean.getBoolean("simplymirrorsplus.visualCheckMotion") && requested==0 && simplymirrorsplus$motionPhase!=2) {
            Path dir=mc.gameDirectory.toPath().resolve("visual-evidence");
            Files.createDirectories(dir);
            if(simplymirrorsplus$motionPhase==0) {
                VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),
                        mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,
                        dir.resolve("mirror-motion-idle.png"));
                // A swing takes several *game ticks*, not two render frames.
                // Capturing immediately after swing() previously recorded the
                // idle pose twice, even though reflected player rendering worked.
                mc.player.swing(InteractionHand.MAIN_HAND);
                simplymirrorsplus$motionStart=simplymirrorsplus$sceneFrames;
                simplymirrorsplus$motionStartPass=MirrorRenderer.completedPasses;
                simplymirrorsplus$motionPhase=1;
                System.out.println("SIMPLY_MIRRORS_ENTITY_SWING_STARTED");
                return;
            }
            float attack=mc.player.getAttackAnim(0.0f);
            int elapsed=simplymirrorsplus$sceneFrames-simplymirrorsplus$motionStart;
            long reflected=MirrorRenderer.completedPasses-simplymirrorsplus$motionStartPass;
            // Wait for the arm to actually travel through the middle of its
            // attack animation and for the mirror to draw the advanced pose.
            // Never accept a frame counter alone as proof of player motion.
            if(attack<0.35f || attack>0.75f || elapsed<3 || reflected<3) {
                if(elapsed>300)
                    throw new IllegalStateException("Reflected player attack animation did not reach mid-swing; attack="+attack+" elapsed="+elapsed+" passes="+reflected);
                return;
            }
            VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),
                    mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,
                    dir.resolve("mirror-motion-swing.png"));
            simplymirrorsplus$motionPhase=2;
            System.out.println("SIMPLY_MIRRORS_ENTITY_SWING_CAPTURED attack="+attack+" elapsedFrames="+elapsed+" reflectionPasses="+reflected);
        }
        if(cameraTest&&simplymirrorsplus$cameraPhase!=4) {
            Path dir=mc.gameDirectory.toPath().resolve("visual-evidence");
            Files.createDirectories(dir);
            long passes=MirrorRenderer.completedPasses;
            if(simplymirrorsplus$cameraPhase==0) {
                VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),
                        mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,
                        dir.resolve("mirror-camera-idle.png"));
                simplymirrorsplus$cameraPhase=1;
                simplymirrorsplus$cameraStartPass=passes;
                System.out.println("SIMPLY_MIRRORS_CAMERA_PAN_STARTED");
                return;
            }
            if(simplymirrorsplus$cameraPhase==1) {
                if(passes<=simplymirrorsplus$cameraStartPass)return;
                VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),
                        mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,
                        dir.resolve("mirror-camera-moving.png"));
                simplymirrorsplus$cameraPhase=2;
                simplymirrorsplus$cameraStartPass=passes;
                return;
            }
            if(simplymirrorsplus$cameraPhase==2) {
                if(passes-simplymirrorsplus$cameraStartPass<20)return;
                VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),
                        mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,
                        dir.resolve("mirror-camera-settled.png"));
                simplymirrorsplus$cameraPhase=3;
                simplymirrorsplus$cameraStartPass=passes;
                System.out.println("SIMPLY_MIRRORS_CAMERA_PAN_CAPTURED passes="+passes);
                return;
            }
            // Return to the exact original pose for the regular six-face screenshot.
            if(passes-simplymirrorsplus$cameraStartPass<5)return;
            simplymirrorsplus$cameraPhase=4;
        }
        boolean tab=false;for(CreativeModeTab t:BuiltInRegistries.CREATIVE_MODE_TAB)if(t!=null&&t.getDisplayName().getString().equals("Simply Mirrors Plus"))tab=true;
        int error=glGetError();Path dir=mc.gameDirectory.toPath().resolve("visual-evidence");Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+requested+".png"));
        String json="{\"passes\":"+(MirrorRenderer.completedPasses-simplymirrorsplus$startPasses)+",\"angleFrames\":"+simplymirrorsplus$mirrorFrames+",\"glError\":"+error+",\"creativeTab\":"+tab+",\"shaderPackActive\":"+com.malidev.simplymirrorsplus.visualsupport.ShaderProbe.active()+",\"stencilEnabled\":"+simplymirrorsplus$stencilReady+",\"motionCaptured\":"+(requested==0&&simplymirrorsplus$motionPhase==2)+",\"cameraMotionCaptured\":"+(cameraTest&&simplymirrorsplus$cameraPhase==4)+"}";
        Files.write(dir.resolve("result-"+requested+".json"),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("SIMPLY_MIRRORS_VISUAL_RESULT "+requested+" "+json);simplymirrorsplus$captured=true;
    }
}
