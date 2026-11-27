package com.malidev.simplymirrorsplus.math;

/**
 * The frame of a framed mirror as an item: a bar along each of the twelve edges of the box the mirror's form
 * fills in its block. The bars are textured like the frame's block would be at the same place, so a frame
 * looks cut from that block.
 */
public final class MirrorItemFrame {
    /** Floats per rectangle: the normal, then four corners as x, y, z and the place in the texture, 0 to 1. */
    public static final int STRIDE=23;
    /** Thickness of a bar, and how far it stands out of the box so it never lies in the mirror's own faces. */
    static final double THICK=2/16d,OUT=1/128d;

    private MirrorItemFrame() { }

    /** The bars around a box given in block coordinates, 0 to 1. */
    public static float[] quads(double x0,double y0,double z0,double x1,double y1,double z1) {
        double[] low={x0,y0,z0},high={x1,y1,z1};
        float[] quads=new float[12*6*STRIDE];
        int at=0;
        for(int axis=0;axis<3;axis++) {
            int b=(axis+1)%3,c=(axis+2)%3;
            for(int corner=0;corner<4;corner++) {
                double[] from=new double[3],to=new double[3];
                from[axis]=low[axis]-OUT;to[axis]=high[axis]+OUT;
                side(low,high,b,corner%2==0,from,to);
                side(low,high,c,corner<2,from,to);
                for(int face=0;face<6;face++)at=face(quads,at,from,to,face/2,face%2==1);
            }
        }
        return quads;
    }

    /** One bar's extent across the box on one axis: a strip at the low or the high end, no wider than the box. */
    private static void side(double[] low,double[] high,int axis,boolean atLow,double[] from,double[] to) {
        double thick=Math.min(THICK,high[axis]-low[axis]);
        from[axis]=atLow?low[axis]-OUT:high[axis]-thick;
        to[axis]=atLow?low[axis]+thick:high[axis]+OUT;
    }

    private static int face(float[] quads,int at,double[] from,double[] to,int axis,boolean positive) {
        int u=(axis+1)%3,v=(axis+2)%3;
        quads[at+axis]=positive?1:-1;
        // Counter-clockwise seen from outside, so the face is not culled.
        double[][] corners=positive?new double[][]{{0,0},{1,0},{1,1},{0,1}}:new double[][]{{0,0},{0,1},{1,1},{1,0}};
        for(int i=0;i<4;i++) {
            double[] p=new double[3];
            p[axis]=positive?to[axis]:from[axis];
            p[u]=corners[i][0]==0?from[u]:to[u];
            p[v]=corners[i][1]==0?from[v]:to[v];
            int o=at+3+i*5;
            quads[o]=(float)p[0];quads[o+1]=(float)p[1];quads[o+2]=(float)p[2];
            // The texture lies on a face as it does on the block: across the two axes the face spans, upright on the sides.
            double across=axis==0?p[2]:p[0],along=axis==1?p[2]:p[1];
            quads[o+3]=(float)Math.max(0,Math.min(1,across));quads[o+4]=(float)Math.max(0,Math.min(1,along));
        }
        return at+STRIDE;
    }
}
