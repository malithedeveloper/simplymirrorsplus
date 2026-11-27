package com.malidev.simplymirrorsplus.client;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

/**
 * The two parts of the mirror settings screen that the game's own buttons cannot show: the place of the live
 * preview and the colour picker. The picker paints itself into an image the size of the window and takes plain
 * mouse input, so it is the same on every Minecraft version; {@link GlMirrorUi} puts the image on screen.
 */
public final class MirrorUi {
    /** One mirror the player can select. */
    public static final class Entry {
        public final String id;
        public final int x,y,z,blocks;
        public final double distance;
        public Entry(String id,int x,int y,int z,int blocks,double distance) {
            this.id=id;this.x=x;this.y=y;this.z=z;this.blocks=blocks;this.distance=distance;
        }
    }

    private static final int[] PRESETS={0xFFFFFF,0x0B0B12,0xFF4D6D,0xFF9F1C,0xFFD60A,0x2DC653,0x00B4D8,0x4361EE,0x7C5CFF,0xF72585};
    private static final List<Integer> RECENT=new ArrayList<Integer>();

    private BufferedImage image;
    private Graphics2D g;
    private int width,height;
    private double mouseX,mouseY;
    private boolean down,pressed,released,dirty=true;
    private String active="";
    private final int[] preview=new int[4],space=new int[4],picker=new int[4];
    private boolean picking;
    private int color;
    private float[] hsv={0,0,1};
    private int hsvOf=-1;

    /** Where the live preview goes, as x, y, width, height in pixels from the top left; width 0 for none. */
    public int[] previewArea() { return preview; }
    /** The room the preview may use, as x, y, width, height in pixels. */
    public int[] previewSpace() { return space; }
    public int[] pixels() { return ((DataBufferInt)image.getRaster().getDataBuffer()).getData(); }

    /**
     * Gives the preview its room and fits the picture into it.
     * @param aspect width over height of the mirror in the picture, 0 while there is none
     */
    public void preview(int x,int y,int w,int h,float aspect) {
        space[0]=x;space[1]=y;space[2]=w;space[3]=h;
        int pw=aspect<=0||w<8||h<8?0:(int)Math.min(w,h*aspect),ph=pw==0?0:(int)(pw/aspect);
        preview[0]=x+(w-pw)/2;preview[1]=y+(h-ph)/2;preview[2]=pw;preview[3]=ph;
    }

    /** Shows the colour picker in this area, editing this colour; a width of 0 hides it. */
    public void picker(int x,int y,int w,int h,int rgb) {
        boolean show=w>0&&h>0;
        if(show!=picking||x!=picker[0]||y!=picker[1]||w!=picker[2]||h!=picker[3]||show&&rgb!=color)dirty=true;
        picking=show;picker[0]=x;picker[1]=y;picker[2]=w;picker[3]=h;
        if(show)color=rgb&0xFFFFFF;
    }

    /** The colour as the picker has it now. */
    public int color() { return color; }

    public void mouse(double x,double y) {
        if(picking&&(x!=mouseX||y!=mouseY))dirty=true;
        mouseX=x;mouseY=y;
    }

    /** The left button went down or up. A click shorter than a frame still counts: both edges stay until the next paint. */
    public void button(boolean isDown) {
        if(isDown)pressed=true;else released=true;
        down=isDown;dirty=true;
    }

    /** Paints the image if anything changed. Returns true when {@link #pixels()} has new content. */
    public boolean paint(int w,int h) {
        if(image==null||w!=width||h!=height) {
            width=w;height=h;
            image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);
            dirty=true;
        }
        if(!dirty)return false;
        dirty=false;
        g=image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,w,h);g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(picking)color=pick(picker[0],picker[1],picker[2],picker[3],color);
        } finally {
            g.dispose();
        }
        pressed=released=false;
        if(!down)active="";
        return true;
    }

    /** Saturation and brightness square, hue bar and two rows of swatches. Returns the edited colour. */
    private int pick(float x,float y,float w,float h,int color) {
        // Hue and saturation are kept apart from the colour, which loses them at black and white.
        if(hsvOf!=color) { Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,hsv);hsvOf=color; }
        float unit=h/10,square=unit*6,bar=unit,gap=unit/2,swatch=unit;
        int hue=Color.HSBtoRGB(hsv[0],1,1);
        g.setColor(new Color(hue));g.fillRect((int)x,(int)y,(int)w,(int)square);
        g.setPaint(new GradientPaint(x,0,Color.WHITE,x+w,0,new Color(0x00FFFFFF,true)));g.fillRect((int)x,(int)y,(int)w,(int)square);
        g.setPaint(new GradientPaint(0,y,new Color(0,true),0,y+square,Color.BLACK));g.fillRect((int)x,(int)y,(int)w,(int)square);
        if(grab("square",x,y,w,square)) { hsv[1]=clamp((float)(mouseX-x)/w);hsv[2]=1-clamp((float)(mouseY-y)/square); }
        ring(x+hsv[1]*w,y+(1-hsv[2])*square,unit*.35f);
        float hy=y+square+gap;
        float[] at={0,1/6f,2/6f,3/6f,4/6f,5/6f,1};
        Color[] hues=new Color[7];
        for(int i=0;i<7;i++)hues[i]=new Color(Color.HSBtoRGB(at[i],1,1));
        g.setPaint(new LinearGradientPaint(x,0,x+w,0,at,hues));g.fillRect((int)x,(int)hy,(int)w,(int)bar);
        if(grab("hue",x,hy,w,bar))hsv[0]=clamp((float)(mouseX-x)/w)*.9999f;
        ring(x+hsv[0]*w,hy+bar/2,unit*.4f);
        int result=Color.HSBtoRGB(hsv[0],hsv[1],hsv[2])&0xFFFFFF;
        float sy=hy+bar+gap,size=(w-9*gap/2)/10;
        for(int i=0;i<PRESETS.length;i++)result=swatch(x+i*(size+gap/2),sy,size,swatch,PRESETS[i],result);
        for(int i=0;i<RECENT.size();i++)result=swatch(x+i*(size+gap/2),sy+swatch+gap/2,size,swatch,RECENT.get(i),result);
        if(released&&result!=color&&!RECENT.contains(result)) { RECENT.add(0,result);if(RECENT.size()>10)RECENT.remove(10); }
        hsvOf=result;
        return result;
    }

    private int swatch(float x,float y,float w,float h,int rgb,int current) {
        g.setColor(new Color(rgb));g.fillRect((int)x,(int)y,(int)w,(int)h);
        if(inside(x,y,w,h)&&pressed) { Color.RGBtoHSB(rgb>>16&255,rgb>>8&255,rgb&255,hsv);return rgb; }
        return current;
    }

    private void ring(float x,float y,float radius) {
        g.setStroke(new BasicStroke(Math.max(1,radius*.4f)));
        g.setColor(Color.BLACK);g.draw(new java.awt.geom.Ellipse2D.Float(x-radius-1,y-radius-1,2*radius+2,2*radius+2));
        g.setColor(Color.WHITE);g.draw(new java.awt.geom.Ellipse2D.Float(x-radius,y-radius,2*radius,2*radius));
    }

    private boolean inside(float x,float y,float w,float h) { return mouseX>=x&&mouseX<x+w&&mouseY>=y&&mouseY<y+h; }

    /** True while the mouse, pressed inside the area, is still held; the area keeps the drag when the mouse leaves it. */
    private boolean grab(String id,float x,float y,float w,float h) {
        if(pressed&&inside(x,y,w,h)&&active.isEmpty())active=id;
        return down&&active.equals(id);
    }

    private static float clamp(float value) { return Math.max(0,Math.min(1,value)); }
}
