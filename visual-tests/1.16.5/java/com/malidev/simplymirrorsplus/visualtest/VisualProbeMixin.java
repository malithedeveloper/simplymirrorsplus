package com.malidev.simplymirrorsplus.visualtest;
import com.malidev.simplymirrorsplus.client.MirrorRenderer;
import com.malidev.simplymirrorsplus.visualsupport.VisualCapture;
import com.malidev.simplymirrorsplus.visualsupport.VisualScene;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.Registry;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33.*;
@Mixin(Minecraft.class)
public abstract class VisualProbeMixin {
    @Unique private long simplymirrorsplus$measureStart,simplymirrorsplus$measurePasses;
    @Unique private int simplymirrorsplus$measureFrames;
    @Unique private java.util.Properties simplymirrorsplus$feature;
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
        if(simplymirrorsplus$frames++==0) {mc.options.pauseOnLostFocus=false;mc.options.renderDistance=4;mc.options.bobView=false;mc.options.framerateLimit=30;mc.options.hideGui=true;}
        if(mc.player==null||mc.level==null) {
            if(simplymirrorsplus$frames%300==0)System.out.println("SIMPLY_MIRRORS_VISUAL_STAGE "+(mc.screen==null?"none":mc.screen.getClass().getName()));
            if(!simplymirrorsplus$joining&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen) {
                simplymirrorsplus$joining=true;String port=System.getProperty("simplymirrorsplus.visualPort");String address="127.0.0.1:"+port;
                System.out.println("SIMPLY_MIRRORS_VISUAL_CONNECT "+address);mc.setScreen(new ConnectScreen(new TitleScreen(),mc,"127.0.0.1",Integer.parseInt(port)));
            }
            return;
        }
        mc.player.abilities.flying=true;
        int requested=VisualScene.index(mc.gameDirectory.toPath());
        if(requested<0)return;
        if(requested==6){simplymirrorsplus$done=true;mc.stop();return;}
        if(requested!=simplymirrorsplus$scene){simplymirrorsplus$scene=requested;simplymirrorsplus$mirrorFrames=0;simplymirrorsplus$sceneFrames=0;simplymirrorsplus$startPasses=MirrorRenderer.completedPasses;simplymirrorsplus$captured=false;simplymirrorsplus$measureStart=0;if(requested>=10)simplymirrorsplus$applyFeature(mc);}
        simplymirrorsplus$sceneFrames++;
        if(MirrorRenderer.completedPasses>simplymirrorsplus$startPasses)simplymirrorsplus$mirrorFrames++;
        if(requested>=10) { simplymirrorsplus$featureFrame(mc,requested);return; }
        int phase=simplymirrorsplus$mirrorFrames/20;
        float yaw=VisualScene.yaw(requested)+(phase==1?20:phase==2?-20:0);
        float base=VisualScene.pitch(requested);
        float pitch=base+(phase==3?(base>0?-20:20):phase==4?(base<0?20:-20):0);
        double[] eye=VisualScene.eye(requested);double px=eye[0],py=eye[1]-1.62,pz=eye[2];
        mc.player.setPos(px,py,pz);mc.player.xo=px;mc.player.yo=py;mc.player.zo=pz;
        mc.player.yRot=yaw;mc.player.xRot=pitch;mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
        if(simplymirrorsplus$captured)return;
        if((MirrorRenderer.completedPasses-simplymirrorsplus$startPasses<120||simplymirrorsplus$mirrorFrames<120)&&simplymirrorsplus$sceneFrames<1800)return;
        boolean tab=false;for(CreativeModeTab t:CreativeModeTab.TABS)if(t!=null&&t.getDisplayName().getString().equals("Simply Mirrors Plus"))tab=true;
        int error=glGetError();Path dir=mc.gameDirectory.toPath().resolve("visual-evidence");Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+requested+".png"));
        String json="{\"passes\":"+(MirrorRenderer.completedPasses-simplymirrorsplus$startPasses)+",\"angleFrames\":"+simplymirrorsplus$mirrorFrames+",\"glError\":"+error+",\"creativeTab\":"+tab+",\"shaderPackActive\":"+com.malidev.simplymirrorsplus.visualsupport.ShaderProbe.active()+"}";
        Files.write(dir.resolve("result-"+requested+".json"),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("SIMPLY_MIRRORS_VISUAL_RESULT "+requested+" "+json);simplymirrorsplus$captured=true;
    }

    @Unique private void simplymirrorsplus$applyFeature(Minecraft mc) throws Exception {
        java.util.Properties values=new java.util.Properties();
        try(java.io.InputStream in=java.nio.file.Files.newInputStream(mc.gameDirectory.toPath().resolve("visual-feature.properties"))) { values.load(in); }
        simplymirrorsplus$feature=values;
        com.malidev.simplymirrorsplus.client.MirrorConfig.enabled=Boolean.parseBoolean(values.getProperty("enabled","true"));
        com.malidev.simplymirrorsplus.client.MirrorConfig.playersOnly=Boolean.parseBoolean(values.getProperty("playersOnly","false"));
        com.malidev.simplymirrorsplus.client.MirrorConfig.resolutionPercent=Integer.parseInt(values.getProperty("resolution","100"));
        com.malidev.simplymirrorsplus.client.MirrorConfig.refreshRate=Integer.parseInt(values.getProperty("fps","60"));
        mc.options.framerateLimit=260;mc.options.enableVsync=false;mc.getWindow().updateVsync(false);
        net.minecraft.client.GraphicsStatus graphics=Boolean.parseBoolean(values.getProperty("fabulous","false"))?net.minecraft.client.GraphicsStatus.FABULOUS:net.minecraft.client.GraphicsStatus.FANCY;
        if(mc.options.graphicsMode!=graphics) { mc.options.graphicsMode=graphics;mc.levelRenderer.allChanged(); }
    }

    @Unique private void simplymirrorsplus$featureFrame(Minecraft mc,int scene) throws Exception {
        java.util.Properties p=simplymirrorsplus$feature;
        java.nio.file.Path root=mc.gameDirectory.toPath();
        float yaw=Float.parseFloat(p.getProperty("yaw"));
        float pitch=Float.parseFloat(p.getProperty("pitch","0"));
        double x=Double.parseDouble(p.getProperty("x")),y=Double.parseDouble(p.getProperty("y"))-1.62,z=Double.parseDouble(p.getProperty("z"));
        mc.player.setPos(x,y,z);mc.player.xo=x;mc.player.yo=y;mc.player.zo=z;
        mc.player.yRot=yaw;mc.player.xRot=pitch;mc.player.yRotO=yaw;mc.player.xRotO=pitch;mc.player.yBodyRot=yaw;mc.player.yHeadRot=yaw;
        if(Boolean.parseBoolean(p.getProperty("menu","false"))) { simplymirrorsplus$menu(mc,root,scene);return; }
        if(simplymirrorsplus$sceneFrames<90)return;
        java.nio.file.Path dir=root.resolve("visual-evidence");
        if(java.nio.file.Files.exists(dir.resolve("result-"+scene+".json")))return;
        long now=System.nanoTime();
        if(simplymirrorsplus$measureStart==0) {
            while(org.lwjgl.opengl.GL11.glGetError()!=org.lwjgl.opengl.GL11.GL_NO_ERROR) { }
            simplymirrorsplus$measureStart=now;simplymirrorsplus$measurePasses=MirrorRenderer.completedPasses;simplymirrorsplus$measureFrames=0;
            return;
        }
        simplymirrorsplus$measureFrames++;
        if(now-simplymirrorsplus$measureStart<3_000_000_000L)return;
        double seconds=(now-simplymirrorsplus$measureStart)/1e9;
        int error=org.lwjgl.opengl.GL11.glGetError();
        java.nio.file.Files.createDirectories(dir);
        VisualCapture.save(mc.getMainRenderTarget().getColorTextureId(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height,dir.resolve("mirror-"+scene+".png"));
        String json="{\"reflectionsPerSecond\":"+(MirrorRenderer.completedPasses-simplymirrorsplus$measurePasses)/seconds+",\"framesPerSecond\":"+simplymirrorsplus$measureFrames/seconds+",\"glError\":"+error+"}";
        java.nio.file.Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Drives the real settings screen with mouse events, then checks the values reached the config. */
    @Unique private void simplymirrorsplus$menu(Minecraft mc,java.nio.file.Path root,int scene) throws Exception {
        java.nio.file.Path dir=root.resolve("visual-evidence");
        if(java.nio.file.Files.exists(dir.resolve("result-"+scene+".json"))||simplymirrorsplus$sceneFrames<90)return;
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),x=w/2-100,y=h/2-72;
        if(simplymirrorsplus$measureStart==0) {
            simplymirrorsplus$measureStart=1;
            mc.setScreen(new com.malidev.simplymirrorsplus.client.MirrorSettingsScreen());
            return;
        }
        if(simplymirrorsplus$measureStart!=1)return;
        simplymirrorsplus$measureStart=2;
        java.nio.file.Files.createDirectories(dir);
        net.minecraft.client.gui.screens.Screen screen=mc.screen;
        boolean enabledBefore=com.malidev.simplymirrorsplus.client.MirrorConfig.enabled,playersBefore=com.malidev.simplymirrorsplus.client.MirrorConfig.playersOnly;
        simplymirrorsplus$click(screen,x+100,y+10);
        simplymirrorsplus$click(screen,x+196,y+24+10);
        simplymirrorsplus$click(screen,x+70,y+48+10);
        int rateAtMiddle=com.malidev.simplymirrorsplus.client.MirrorConfig.refreshRate;
        simplymirrorsplus$click(screen,x+196,y+48+10);
        simplymirrorsplus$click(screen,x+100,y+72+10);
        boolean key=false;
        for(net.minecraft.client.KeyMapping mapping:mc.options.keyMappings)if(mapping.getName().equals("key.simplymirrorsplus.settings"))key=true;
        String json="{\"enabledFlipped\":"+(enabledBefore!=com.malidev.simplymirrorsplus.client.MirrorConfig.enabled)+",\"playersFlipped\":"+(playersBefore!=com.malidev.simplymirrorsplus.client.MirrorConfig.playersOnly)+",\"resolution\":"+com.malidev.simplymirrorsplus.client.MirrorConfig.resolutionPercent+",\"rateAtMiddle\":"+rateAtMiddle+",\"rate\":"+com.malidev.simplymirrorsplus.client.MirrorConfig.refreshRate+",\"keyRegistered\":"+key+"}";
        java.nio.file.Files.write(dir.resolve("result-"+scene+".json"),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        screen.onClose();
    }

    @Unique private static void simplymirrorsplus$click(net.minecraft.client.gui.screens.Screen screen,double x,double y) { screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0); }
}
