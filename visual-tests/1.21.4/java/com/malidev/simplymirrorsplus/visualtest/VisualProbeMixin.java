package com.malidev.simplymirrorsplus.visualtest;

import com.malidev.simplymirrorsplus.client.MirrorConfig;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.visualsupport.VisualCapture;
import com.malidev.simplymirrorsplus.visualsupport.VisualScene;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.lwjgl.opengl.GL33.*;

/** Scenes 0-5 are the shared six-face fixture; scenes 10+ take their pose and settings from visual-feature.properties. */
@Mixin(Minecraft.class)
public abstract class VisualProbeMixin {
    @Unique private static final int SETTLE_FRAMES=90;
    @Unique private static final long MEASURE_NANOS=3_000_000_000L;
    @Unique private int simplymirrorsplus$frames,simplymirrorsplus$mirrorFrames,simplymirrorsplus$scene=-1,simplymirrorsplus$sceneFrames,simplymirrorsplus$measureFrames;
    @Unique private long simplymirrorsplus$startPasses,simplymirrorsplus$measureStart,simplymirrorsplus$measurePasses;
    @Unique private boolean simplymirrorsplus$captured,simplymirrorsplus$done,simplymirrorsplus$joining;
    @Unique private Properties simplymirrorsplus$feature;

    @Inject(method="runTick",at=@At("TAIL"))
    private void simplymirrorsplus$probe(CallbackInfo ci) throws Exception {
        if(simplymirrorsplus$done)return;
        Minecraft mc=Minecraft.getInstance();
        if(simplymirrorsplus$frames++==0) {
            mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.bobView().set(false);mc.options.hideGui=true;
            mc.options.framerateLimit().set(260);mc.options.enableVsync().set(false);mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        }
        if(mc.player==null||mc.level==null) {
            if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
            if(!simplymirrorsplus$joining&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen) {
                simplymirrorsplus$joining=true;
                String address="127.0.0.1:"+System.getProperty("simplymirrorsplus.visualPort");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Mirror test",address,ServerData.Type.OTHER),false,null);
            }
            return;
        }
        mc.player.getAbilities().flying=true;
        Path root=mc.gameDirectory.toPath();
        int requested=VisualScene.index(root);
        if(requested<0)return;
        if(requested==6) { simplymirrorsplus$done=true;mc.stop();return; }
        if(requested!=simplymirrorsplus$scene) {
            simplymirrorsplus$scene=requested;simplymirrorsplus$mirrorFrames=0;simplymirrorsplus$sceneFrames=0;simplymirrorsplus$startPasses=com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses;simplymirrorsplus$captured=false;simplymirrorsplus$measureStart=0;
            if(requested>=10)simplymirrorsplus$applyFeature(root);
        }
        simplymirrorsplus$sceneFrames++;
        if(com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses>simplymirrorsplus$startPasses)simplymirrorsplus$mirrorFrames++;
        if(requested>=10) { simplymirrorsplus$featureFrame(mc,root,requested);return; }
        int phase=simplymirrorsplus$mirrorFrames/20;
        float base=VisualScene.pitch(requested);
        simplymirrorsplus$pose(mc,VisualScene.eye(requested),VisualScene.yaw(requested)+(phase==1?20:phase==2?-20:0),base+(phase==3?(base>0?-20:20):phase==4?(base<0?20:-20):0));
        if(simplymirrorsplus$captured)return;
        if((com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses-simplymirrorsplus$startPasses<120||simplymirrorsplus$mirrorFrames<120)&&simplymirrorsplus$sceneFrames<1800)return;
        boolean tab=false;
        for(CreativeModeTab t:BuiltInRegistries.CREATIVE_MODE_TAB)if(t!=null&&t.getDisplayName().getString().equals("Simply Mirrors Plus"))tab=true;
        int error=glGetError();
        Path dir=root.resolve("visual-evidence");
        Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+requested+".png"));
        String json="{\"passes\":"+(com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses-simplymirrorsplus$startPasses)+",\"angleFrames\":"+simplymirrorsplus$mirrorFrames+",\"glError\":"+error+",\"creativeTab\":"+tab+"}";
        Files.write(dir.resolve("result-"+requested+".json"),json.getBytes(StandardCharsets.UTF_8));
        simplymirrorsplus$captured=true;
    }

    @Unique private void simplymirrorsplus$applyFeature(Path root) throws Exception {
        Properties values=new Properties();
        try(InputStream in=Files.newInputStream(root.resolve("visual-feature.properties"))) { values.load(in); }
        simplymirrorsplus$feature=values;
        MirrorConfig.enabled=Boolean.parseBoolean(values.getProperty("enabled","true"));
        MirrorConfig.playersOnly=Boolean.parseBoolean(values.getProperty("playersOnly","false"));
        MirrorConfig.resolutionPercent=Integer.parseInt(values.getProperty("resolution","100"));
        MirrorConfig.refreshRate=Integer.parseInt(values.getProperty("fps","60"));
        Minecraft mc=Minecraft.getInstance();
        net.minecraft.client.GraphicsStatus graphics=Boolean.parseBoolean(values.getProperty("fabulous","false"))?net.minecraft.client.GraphicsStatus.FABULOUS:net.minecraft.client.GraphicsStatus.FANCY;
        if(mc.options.graphicsMode().get()!=graphics) { mc.options.graphicsMode().set(graphics);mc.levelRenderer.allChanged(); }
    }

    @Unique private void simplymirrorsplus$featureFrame(Minecraft mc,Path root,int scene) throws Exception {
        Properties p=simplymirrorsplus$feature;
        float yaw=Float.parseFloat(p.getProperty("yaw"))+Float.parseFloat(p.getProperty("spin","0"))*(float)Math.sin(simplymirrorsplus$sceneFrames*.25);
        simplymirrorsplus$pose(mc,new double[]{Double.parseDouble(p.getProperty("x")),Double.parseDouble(p.getProperty("y")),Double.parseDouble(p.getProperty("z"))},yaw,Float.parseFloat(p.getProperty("pitch","0")));
        if(Boolean.parseBoolean(p.getProperty("menu","false"))) { simplymirrorsplus$menu(mc,root,scene);return; }
        if(Boolean.parseBoolean(p.getProperty("use","false"))) simplymirrorsplus$sneakUse(mc);
        if(simplymirrorsplus$captured||simplymirrorsplus$sceneFrames<SETTLE_FRAMES)return;
        long now=System.nanoTime();
        if(simplymirrorsplus$measureStart==0) {
            while(glGetError()!=GL_NO_ERROR) { }
            simplymirrorsplus$measureStart=now;simplymirrorsplus$measurePasses=com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses;simplymirrorsplus$measureFrames=0;
            return;
        }
        simplymirrorsplus$measureFrames++;
        if(now-simplymirrorsplus$measureStart<MEASURE_NANOS)return;
        double seconds=(now-simplymirrorsplus$measureStart)/1e9;
        int error=glGetError();
        Path dir=root.resolve("visual-evidence");
        Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+scene+".png"));
        String json="{\"reflectionsPerSecond\":"+(com.malidev.simplymirrorsplus.client.MirrorEngine.completedPasses-simplymirrorsplus$measurePasses)/seconds+",\"framesPerSecond\":"+simplymirrorsplus$measureFrames/seconds+",\"glError\":"+error+"}";
        Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(StandardCharsets.UTF_8));
        simplymirrorsplus$captured=true;
    }

    /** Sneak and right-click the middle of mirror A through the real interaction packets. */
    @Unique private void simplymirrorsplus$sneakUse(Minecraft mc) {
        if(simplymirrorsplus$sceneFrames==SETTLE_FRAMES/3)
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        if(simplymirrorsplus$sceneFrames==SETTLE_FRAMES/2) {
            net.minecraft.core.BlockPos pos=new net.minecraft.core.BlockPos(0,66,0);
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5,66.5,0),net.minecraft.core.Direction.NORTH,pos,false));
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        }
    }

    /** Drives the real settings screen with mouse events, then checks the values reached the config file. */
    @Unique private void simplymirrorsplus$menu(Minecraft mc,Path root,int scene) throws Exception {
        if(simplymirrorsplus$captured||simplymirrorsplus$sceneFrames<SETTLE_FRAMES)return;
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),x=w/2-100,y=h/2-72;
        if(simplymirrorsplus$measureStart==0) {
            simplymirrorsplus$measureStart=1;
            mc.setScreen(new com.malidev.simplymirrorsplus.client.MirrorSettingsScreen());
            return;
        }
        if(simplymirrorsplus$measureStart==1) {
            simplymirrorsplus$measureStart=2;
            Path dir=root.resolve("visual-evidence");
            Files.createDirectories(dir);
            VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("menu-open.png"));
            net.minecraft.client.gui.screens.Screen screen=mc.screen;
            boolean enabledBefore=MirrorConfig.enabled,playersBefore=MirrorConfig.playersOnly;
            screen.mouseClicked(x+100,y+10,0);screen.mouseReleased(x+100,y+10,0);
            screen.mouseClicked(x+196,y+24+10,0);screen.mouseReleased(x+196,y+24+10,0);
            screen.mouseClicked(x+70,y+48+10,0);screen.mouseReleased(x+70,y+48+10,0);
            int rateAfterMiddle=MirrorConfig.refreshRate;
            screen.mouseClicked(x+196,y+48+10,0);screen.mouseReleased(x+196,y+48+10,0);
            screen.mouseClicked(x+100,y+72+10,0);screen.mouseReleased(x+100,y+72+10,0);
            boolean key=false;
            for(net.minecraft.client.KeyMapping mapping:mc.options.keyMappings)if(mapping.getName().equals("key.simplymirrorsplus.settings"))key=true;
            String json="{\"enabledFlipped\":"+(enabledBefore!=MirrorConfig.enabled)+",\"playersFlipped\":"+(playersBefore!=MirrorConfig.playersOnly)+",\"resolution\":"+MirrorConfig.resolutionPercent+",\"rateAtMiddle\":"+rateAfterMiddle+",\"rate\":"+MirrorConfig.refreshRate+",\"keyRegistered\":"+key+"}";
            Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(StandardCharsets.UTF_8));
            screen.onClose();
            simplymirrorsplus$captured=true;
        }
    }

    @Unique private static void simplymirrorsplus$pose(Minecraft mc,double[] eye,float yaw,float pitch) {
        double x=eye[0],y=eye[1]-1.62,z=eye[2];
        mc.player.setPos(x,y,z);mc.player.xo=x;mc.player.yo=y;mc.player.zo=z;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
    }
}
