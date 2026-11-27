package com.malidev.simplymirrorsplus.client;

import java.util.Locale;
import java.util.Properties;

/** What a Players Only reflection shows behind the players. Plain values, so each mirror can carry its own. */
public final class MirrorBackdrop {
    /** The names are the values in the config file. */
    public enum Kind { END, BLOCK, COLOR, GRADIENT, IMAGE, PANORAMA }
    /** How a gradient moves. */
    public enum Motion { NONE, SCROLL, ROTATE, PULSE, RAINBOW }

    public Kind kind=Kind.END;
    /** 0xRRGGBB. The plain colour, and where a gradient starts. */
    public int color=0x1B1B2F;
    /** 0xRRGGBB, where a gradient ends. */
    public int color2=0x7A3CFF;
    /** Direction of a straight gradient in degrees; 0 runs left to right, 90 bottom to top. */
    public int angle=90;
    /** A gradient that grows from the middle of the mirror instead of running across it. */
    public boolean radial;
    public Motion motion=Motion.NONE;
    /** Cycles per minute of the motion. */
    public int speed=12;
    /** File name inside the backgrounds folder, for the picture and panorama kinds. */
    public String image="";
    /** Id of the block whose texture fills the block kind. */
    public String block="minecraft:birch_planks";

    public void read(Properties values,String prefix) {
        kind=named(Kind.class,values.getProperty(prefix),kind);
        color=color(values.getProperty(prefix+"Color"),color);
        color2=color(values.getProperty(prefix+"Color2"),color2);
        angle=integer(values.getProperty(prefix+"Angle"),angle);
        radial=Boolean.parseBoolean(values.getProperty(prefix+"Radial",String.valueOf(radial)));
        motion=named(Motion.class,values.getProperty(prefix+"Motion"),motion);
        speed=Math.max(1,Math.min(240,integer(values.getProperty(prefix+"Speed"),speed)));
        image=values.getProperty(prefix+"Image",image);
        block=values.getProperty(prefix+"Block",block);
    }

    public void write(Properties values,String prefix) {
        values.setProperty(prefix,kind.name().toLowerCase(Locale.ROOT));
        values.setProperty(prefix+"Color",hex(color));
        values.setProperty(prefix+"Color2",hex(color2));
        values.setProperty(prefix+"Angle",String.valueOf(angle));
        values.setProperty(prefix+"Radial",String.valueOf(radial));
        values.setProperty(prefix+"Motion",motion.name().toLowerCase(Locale.ROOT));
        values.setProperty(prefix+"Speed",String.valueOf(speed));
        values.setProperty(prefix+"Image",image);
        values.setProperty(prefix+"Block",block);
    }

    public void set(MirrorBackdrop other) {
        kind=other.kind;color=other.color;color2=other.color2;angle=other.angle;radial=other.radial;
        motion=other.motion;speed=other.speed;image=other.image;block=other.block;
    }

    public static String hex(int color) { return String.format("#%06X",color&0xFFFFFF); }

    /** Reads #RRGGBB or RRGGBB. */
    public static int color(String text,int fallback) {
        try { return text==null?fallback:Integer.parseInt(text.trim().replace("#",""),16)&0xFFFFFF; }
        catch(NumberFormatException invalid) { return fallback; }
    }

    private static int integer(String text,int fallback) {
        try { return text==null?fallback:Integer.parseInt(text.trim()); }
        catch(NumberFormatException invalid) { return fallback; }
    }

    static <E extends Enum<E>> E named(Class<E> type,String name,E fallback) {
        try { return name==null?fallback:Enum.valueOf(type,name.trim().toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException unknown) { return fallback; }
    }
}
