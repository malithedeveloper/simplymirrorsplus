package com.malidev.simplymirrorsplus.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Client-only settings, read by the renderer every frame. */
public final class MirrorConfig {
    public static final int[] REFRESH_RATES={30,60,90,120};
    public static final int MIN_RESOLUTION=25;
    public static final int MAX_RESOLUTION=100;

    public static boolean enabled=true;
    public static boolean playersOnly=false;
    public static int resolutionPercent=100;
    public static int refreshRate=60;

    private static final Logger LOGGER=Logger.getLogger("simplymirrorsplus");
    private static Path file;

    private MirrorConfig() { }

    public static void load(Path path) {
        file=path;
        if(!Files.isRegularFile(path))return;
        Properties values=new Properties();
        try(InputStream in=Files.newInputStream(path)) {
            values.load(in);
        } catch(IOException|IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING,"Could not read "+path+"; using default mirror settings",exception);
            return;
        }
        enabled=Boolean.parseBoolean(values.getProperty("enabled",String.valueOf(enabled)));
        playersOnly=Boolean.parseBoolean(values.getProperty("playersOnly",String.valueOf(playersOnly)));
        resolutionPercent=Math.clamp(integer(values,"resolutionPercent",resolutionPercent),MIN_RESOLUTION,MAX_RESOLUTION);
        refreshRate=nearestRate(integer(values,"refreshRate",refreshRate));
    }

    public static void save() {
        if(file==null)return;
        Properties values=new Properties();
        values.setProperty("enabled",String.valueOf(enabled));
        values.setProperty("playersOnly",String.valueOf(playersOnly));
        values.setProperty("resolutionPercent",String.valueOf(resolutionPercent));
        values.setProperty("refreshRate",String.valueOf(refreshRate));
        try {
            Files.createDirectories(file.getParent());
            try(OutputStream out=Files.newOutputStream(file)) { values.store(out,"Simply Mirrors Plus client settings"); }
        } catch(IOException exception) {
            LOGGER.log(Level.WARNING,"Could not save "+file,exception);
        }
    }

    private static int integer(Properties values,String key,int fallback) {
        try { return Integer.parseInt(values.getProperty(key,"").trim()); }
        catch(NumberFormatException invalid) { return fallback; }
    }

    private static int nearestRate(int rate) {
        int best=REFRESH_RATES[0];
        for(int candidate:REFRESH_RATES)if(Math.abs(candidate-rate)<Math.abs(best-rate))best=candidate;
        return best;
    }
}
