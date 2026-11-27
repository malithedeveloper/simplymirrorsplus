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
    /**
     * How much work the mirrors may do. Every level shows the same reflections; a lower one draws fewer of
     * them per frame, at a lower resolution and from less far away.
     */
    public enum Performance {
        LOW(50,2,1,8,3,1,0),MEDIUM(75,3,1,12,4,2,1),HIGH(100,6,1,24,6,2,3),MAX(100,12,2,48,10,3,8);
        /** Percent of the screen's resolution a reflection is drawn at. */
        public final int resolution;
        /** Mirror planes redrawn every frame, nearest first. */
        public final int livePlanes;
        /** Of the remaining planes, how many are redrawn per frame. */
        public final int backgroundUpdates;
        /** Planes that keep an image, and so look like mirrors at all. */
        public final int cachedPlanes;
        /** How far away mirrors are found, in chunks. */
        public final int chunks;
        /** 1 = plain reflection, 2 = a mirror seen inside a reflection reflects too, and so on. */
        public final int bounces;
        /** Reflections inside reflections drawn per frame; mirrors beyond it reuse an image that exists. */
        public final int nestedBudget;
        Performance(int resolution,int livePlanes,int backgroundUpdates,int cachedPlanes,int chunks,int bounces,int nestedBudget) {
            this.resolution=resolution;this.livePlanes=livePlanes;this.backgroundUpdates=backgroundUpdates;this.cachedPlanes=cachedPlanes;
            this.chunks=chunks;this.bounces=bounces;this.nestedBudget=nestedBudget;
        }
    }

    public static boolean enabled=true;
    public static boolean playersOnly=false;
    public static Performance performance=Performance.HIGH;
    public static final MirrorBackdrop backdrop=new MirrorBackdrop();

    private static final Logger LOGGER=Logger.getLogger("simplymirrorsplus");
    /** Mirrors with settings of their own in the open world, by mirror id. */
    private static final java.util.Map<String,MirrorSettings> MIRRORS=new java.util.LinkedHashMap<String,MirrorSettings>();
    private static Path file,worldFile;

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
        performance=MirrorBackdrop.named(Performance.class,values.getProperty("performance"),performance);
        backdrop.read(values,"backdrop");
    }

    public static void save() {
        if(file==null)return;
        Properties values=new Properties();
        values.setProperty("enabled",String.valueOf(enabled));
        values.setProperty("playersOnly",String.valueOf(playersOnly));
        values.setProperty("performance",performance.name().toLowerCase(java.util.Locale.ROOT));
        backdrop.write(values,"backdrop");
        try {
            Files.createDirectories(file.getParent());
            try(OutputStream out=Files.newOutputStream(file)) { values.store(out,"Simply Mirrors Plus client settings"); }
        } catch(IOException exception) {
            LOGGER.log(Level.WARNING,"Could not save "+file,exception);
        }
        saveWorld();
    }

    /** The mirror's own settings, or null while it follows the defaults. */
    public static MirrorSettings mirror(String id) { return MIRRORS.get(id); }

    /** Gives the mirror settings of its own, starting as a copy of the defaults. */
    public static MirrorSettings customize(String id) {
        MirrorSettings settings=MIRRORS.get(id);
        if(settings==null) { settings=new MirrorSettings();MIRRORS.put(id,settings); }
        return settings;
    }

    /** Makes the mirror follow the defaults again. */
    public static void reset(String id) { MIRRORS.remove(id); }

    /** One of the MirrorEngine MODE_ constants: what this mirror shows as far as the settings go. */
    public static int mode(String id) { MirrorSettings own=MIRRORS.get(id);return own!=null?own.mode:playersOnly?MirrorEngine.MODE_PLAYERS:MirrorEngine.MODE_NORMAL; }

    /** Moves a mirror's settings to the id it is known by now. */
    public static void rename(String from,String to) {
        MirrorSettings settings=MIRRORS.remove(from);
        if(settings!=null&&!MIRRORS.containsKey(to))MIRRORS.put(to,settings);
    }

    public static MirrorBackdrop backdrop(String id) { MirrorSettings own=MIRRORS.get(id);return own==null?backdrop:own.backdrop; }

    /**
     * Switches to the per-mirror settings of another world or server.
     * @param key a name for the world; it becomes part of a file name
     */
    public static void openWorld(String key) {
        saveWorld();
        MIRRORS.clear();
        worldFile=null;
        if(file==null||key==null||key.isEmpty())return;
        worldFile=file.resolveSibling("simplymirrorsplus").resolve("mirrors").resolve(key.replaceAll("[^A-Za-z0-9._-]","_")+".properties");
        if(!Files.isRegularFile(worldFile))return;
        Properties values=new Properties();
        try(InputStream in=Files.newInputStream(worldFile)) {
            values.load(in);
        } catch(IOException|IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING,"Could not read "+worldFile,exception);
            return;
        }
        for(String id:values.getProperty("mirrors","").split(";")) {
            if(id.isEmpty())continue;
            MirrorSettings settings=new MirrorSettings();
            settings.read(values,id+".");
            MIRRORS.put(id,settings);
        }
    }

    private static void saveWorld() {
        if(worldFile==null)return;
        try {
            if(MIRRORS.isEmpty()) { Files.deleteIfExists(worldFile);return; }
            Properties values=new Properties();
            StringBuilder ids=new StringBuilder();
            for(java.util.Map.Entry<String,MirrorSettings> entry:MIRRORS.entrySet()) {
                ids.append(entry.getKey()).append(';');
                entry.getValue().write(values,entry.getKey()+".");
            }
            values.setProperty("mirrors",ids.toString());
            Files.createDirectories(worldFile.getParent());
            try(OutputStream out=Files.newOutputStream(worldFile)) { values.store(out,"Simply Mirrors Plus settings of single mirrors"); }
        } catch(IOException exception) {
            LOGGER.log(Level.WARNING,"Could not save "+worldFile,exception);
        }
    }

    /** Where players put their own pictures: config/simplymirrorsplus/backgrounds. Null before the config is loaded. */
    public static Path pictures() { return file==null?null:file.resolveSibling("simplymirrorsplus").resolve("backgrounds"); }
}
