package com.malidev.simplymirrorsplus.visualtest;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
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
    @Unique private boolean simplymirrorsplus$joining;
    @Inject(method="runTick",at=@At("TAIL"))
    private void simplymirrorsplus$probe(CallbackInfo ci) throws Exception {
        if(simplymirrorsplus$done)return;Minecraft mc=Minecraft.getInstance();
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
        if(requested!=simplymirrorsplus$scene){simplymirrorsplus$scene=requested;simplymirrorsplus$mirrorFrames=0;simplymirrorsplus$sceneFrames=0;simplymirrorsplus$startPasses=MirrorRenderer.completedPasses;simplymirrorsplus$captured=false;}
        simplymirrorsplus$sceneFrames++;
        if(MirrorRenderer.completedPasses>simplymirrorsplus$startPasses)simplymirrorsplus$mirrorFrames++;
        int phase=simplymirrorsplus$mirrorFrames/20;
        float yaw=VisualScene.yaw(requested)+(phase==1?20:phase==2?-20:0);
        float base=VisualScene.pitch(requested);
        float pitch=base+(phase==3?(base>0?-20:20):phase==4?(base<0?20:-20):0);
        double[] eye=VisualScene.eye(requested);double px=eye[0],py=eye[1]-1.62,pz=eye[2];
        mc.player.setPos(px,py,pz);mc.player.xo=px;mc.player.yo=py;mc.player.zo=pz;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
        if(simplymirrorsplus$captured)return;
        if((MirrorRenderer.completedPasses-simplymirrorsplus$startPasses<120||simplymirrorsplus$mirrorFrames<120)&&simplymirrorsplus$sceneFrames<1800)return;
        boolean tab=false;for(CreativeModeTab t:BuiltInRegistries.CREATIVE_MODE_TAB)if(t!=null&&t.getDisplayName().getString().equals("Simply Mirrors Plus"))tab=true;
        int error=glGetError();Path dir=mc.gameDirectory.toPath().resolve("visual-evidence");Files.createDirectories(dir);
        VisualCapture.save(((com.mojang.blaze3d.opengl.GlTexture)mc.getMainRenderTarget().getColorTexture()).glId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+requested+".png"));
        String json="{\"passes\":"+(MirrorRenderer.completedPasses-simplymirrorsplus$startPasses)+",\"angleFrames\":"+simplymirrorsplus$mirrorFrames+",\"glError\":"+error+",\"creativeTab\":"+tab+",\"shaderPackActive\":"+com.malidev.simplymirrorsplus.visualsupport.ShaderProbe.active()+"}";
        Files.write(dir.resolve("result-"+requested+".json"),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("SIMPLY_MIRRORS_VISUAL_RESULT "+requested+" "+json);simplymirrorsplus$captured=true;
    }
}
