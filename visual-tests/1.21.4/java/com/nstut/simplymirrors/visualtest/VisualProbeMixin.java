package com.nstut.simplymirrors.visualtest;

import com.nstut.simplymirrors.client.MirrorConfig;
import com.nstut.simplymirrors.client.MirrorRenderer;
import com.nstut.simplymirrors.visualsupport.VisualCapture;
import com.nstut.simplymirrors.visualsupport.VisualScene;
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
    @Unique private int simplymirrors$frames,simplymirrors$mirrorFrames,simplymirrors$scene=-1,simplymirrors$sceneFrames,simplymirrors$measureFrames;
    @Unique private long simplymirrors$startPasses,simplymirrors$measureStart,simplymirrors$measurePasses;
    @Unique private boolean simplymirrors$captured,simplymirrors$done,simplymirrors$joining;
    @Unique private Properties simplymirrors$feature;

    @Inject(method="runTick",at=@At("TAIL"))
    private void simplymirrors$probe(CallbackInfo ci) throws Exception {
        if(simplymirrors$done)return;
        Minecraft mc=Minecraft.getInstance();
        if(simplymirrors$frames++==0) {
            mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.bobView().set(false);mc.options.hideGui=true;
            mc.options.framerateLimit().set(260);mc.options.enableVsync().set(false);mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        }
        if(mc.player==null||mc.level==null) {
            if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
            if(!simplymirrors$joining&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen) {
                simplymirrors$joining=true;
                String address="127.0.0.1:"+System.getProperty("simplymirrors.visualPort");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Mirror test",address,ServerData.Type.OTHER),false,null);
            }
            return;
        }
        mc.player.getAbilities().flying=true;
        Path root=mc.gameDirectory.toPath();
        int requested=VisualScene.index(root);
        if(requested<0)return;
        if(requested==6) { simplymirrors$done=true;mc.stop();return; }
        if(requested!=simplymirrors$scene) {
            simplymirrors$scene=requested;simplymirrors$mirrorFrames=0;simplymirrors$sceneFrames=0;simplymirrors$startPasses=MirrorRenderer.completedPasses;simplymirrors$captured=false;simplymirrors$measureStart=0;
            if(requested>=10)simplymirrors$applyFeature(root);
        }
        simplymirrors$sceneFrames++;
        if(MirrorRenderer.completedPasses>simplymirrors$startPasses)simplymirrors$mirrorFrames++;
        if(requested>=10) { simplymirrors$featureFrame(mc,root,requested);return; }
        int phase=simplymirrors$mirrorFrames/20;
        float base=VisualScene.pitch(requested);
        simplymirrors$pose(mc,VisualScene.eye(requested),VisualScene.yaw(requested)+(phase==1?20:phase==2?-20:0),base+(phase==3?(base>0?-20:20):phase==4?(base<0?20:-20):0));
        if(simplymirrors$captured)return;
        if((MirrorRenderer.completedPasses-simplymirrors$startPasses<120||simplymirrors$mirrorFrames<120)&&simplymirrors$sceneFrames<1800)return;
        boolean tab=false;
        for(CreativeModeTab t:BuiltInRegistries.CREATIVE_MODE_TAB)if(t!=null&&t.getDisplayName().getString().equals("Simply Mirrors"))tab=true;
        int error=glGetError();
        Path dir=root.resolve("visual-evidence");
        Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+requested+".png"));
        String json="{\"passes\":"+(MirrorRenderer.completedPasses-simplymirrors$startPasses)+",\"angleFrames\":"+simplymirrors$mirrorFrames+",\"glError\":"+error+",\"creativeTab\":"+tab+"}";
        Files.write(dir.resolve("result-"+requested+".json"),json.getBytes(StandardCharsets.UTF_8));
        simplymirrors$captured=true;
    }

    @Unique private void simplymirrors$applyFeature(Path root) throws Exception {
        Properties values=new Properties();
        try(InputStream in=Files.newInputStream(root.resolve("visual-feature.properties"))) { values.load(in); }
        simplymirrors$feature=values;
        MirrorConfig.enabled=Boolean.parseBoolean(values.getProperty("enabled","true"));
        MirrorConfig.playersOnly=Boolean.parseBoolean(values.getProperty("playersOnly","false"));
        MirrorConfig.resolutionPercent=Integer.parseInt(values.getProperty("resolution","100"));
        MirrorConfig.refreshRate=Integer.parseInt(values.getProperty("fps","60"));
        Minecraft mc=Minecraft.getInstance();
        net.minecraft.client.GraphicsStatus graphics=Boolean.parseBoolean(values.getProperty("fabulous","false"))?net.minecraft.client.GraphicsStatus.FABULOUS:net.minecraft.client.GraphicsStatus.FANCY;
        if(mc.options.graphicsMode().get()!=graphics) { mc.options.graphicsMode().set(graphics);mc.levelRenderer.allChanged(); }
    }

    @Unique private void simplymirrors$featureFrame(Minecraft mc,Path root,int scene) throws Exception {
        Properties p=simplymirrors$feature;
        float yaw=Float.parseFloat(p.getProperty("yaw"))+Float.parseFloat(p.getProperty("spin","0"))*(float)Math.sin(simplymirrors$sceneFrames*.25);
        simplymirrors$pose(mc,new double[]{Double.parseDouble(p.getProperty("x")),Double.parseDouble(p.getProperty("y")),Double.parseDouble(p.getProperty("z"))},yaw,Float.parseFloat(p.getProperty("pitch","0")));
        if(Boolean.parseBoolean(p.getProperty("menu","false"))) { simplymirrors$menu(mc,root,scene);return; }
        if(simplymirrors$captured||simplymirrors$sceneFrames<SETTLE_FRAMES)return;
        long now=System.nanoTime();
        if(simplymirrors$measureStart==0) {
            while(glGetError()!=GL_NO_ERROR) { }
            simplymirrors$measureStart=now;simplymirrors$measurePasses=MirrorRenderer.completedPasses;simplymirrors$measureFrames=0;
            return;
        }
        simplymirrors$measureFrames++;
        if(now-simplymirrors$measureStart<MEASURE_NANOS)return;
        double seconds=(now-simplymirrors$measureStart)/1e9;
        int error=glGetError();
        Path dir=root.resolve("visual-evidence");
        Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+scene+".png"));
        String json="{\"reflectionsPerSecond\":"+(MirrorRenderer.completedPasses-simplymirrors$measurePasses)/seconds+",\"framesPerSecond\":"+simplymirrors$measureFrames/seconds+",\"glError\":"+error+"}";
        Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(StandardCharsets.UTF_8));
        simplymirrors$captured=true;
    }

    /** Drives the real settings screen with mouse events, then checks the values reached the config file. */
    @Unique private void simplymirrors$menu(Minecraft mc,Path root,int scene) throws Exception {
        if(simplymirrors$captured||simplymirrors$sceneFrames<SETTLE_FRAMES)return;
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),x=w/2-100,y=h/2-72;
        if(simplymirrors$measureStart==0) {
            simplymirrors$measureStart=1;
            mc.setScreen(new com.nstut.simplymirrors.client.MirrorSettingsScreen());
            return;
        }
        if(simplymirrors$measureStart==1) {
            simplymirrors$measureStart=2;
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
            for(net.minecraft.client.KeyMapping mapping:mc.options.keyMappings)if(mapping.getName().equals("key.simply_mirrors.settings"))key=true;
            String json="{\"enabledFlipped\":"+(enabledBefore!=MirrorConfig.enabled)+",\"playersFlipped\":"+(playersBefore!=MirrorConfig.playersOnly)+",\"resolution\":"+MirrorConfig.resolutionPercent+",\"rateAtMiddle\":"+rateAfterMiddle+",\"rate\":"+MirrorConfig.refreshRate+",\"keyRegistered\":"+key+"}";
            Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(StandardCharsets.UTF_8));
            screen.onClose();
            simplymirrors$captured=true;
        }
    }

    @Unique private static void simplymirrors$pose(Minecraft mc,double[] eye,float yaw,float pitch) {
        double x=eye[0],y=eye[1]-1.62,z=eye[2];
        mc.player.setPos(x,y,z);mc.player.xo=x;mc.player.yo=y;mc.player.zo=z;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
    }
}
