package com.malidev.simplymirrorsplus.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The forms a mirror block can have. FULL is the cube whose six faces reflect, SLAB half of it, DOUBLE_SLAB two
 * slabs in one block. Every other form is cut to a 16 by 16 pixel outline that the block models share: as a block
 * of that outline through its whole depth, or, for the names with PANEL, as a plate one pixel thick on one side.
 * The names ending in C1 to C15 are a round mirror joined to its neighbours: each bit makes one corner square.
 */
public enum MirrorShape {
    FULL(Kind.CUBE,-1),SLAB(Kind.SLAB,-1),PANEL(Kind.PLATE,0),CIRCLE(Kind.BLOCK,1),CIRCLE_PANEL(Kind.PLATE,1),HALF_CIRCLE(Kind.BLOCK,2),
    HALF_CIRCLE_PANEL(Kind.PLATE,2),QUARTER_CIRCLE(Kind.BLOCK,3),QUARTER_CIRCLE_PANEL(Kind.PLATE,3),INVERSE_QUARTER_CIRCLE(Kind.BLOCK,4),
    INVERSE_QUARTER_CIRCLE_PANEL(Kind.PLATE,4),TRIANGLE(Kind.BLOCK,5),TRIANGLE_PANEL(Kind.PLATE,5),DIAMOND(Kind.BLOCK,6),DIAMOND_PANEL(Kind.PLATE,6),
    OCTAGON(Kind.BLOCK,7),OCTAGON_PANEL(Kind.PLATE,7),ARCH(Kind.BLOCK,8),ARCH_PANEL(Kind.PLATE,8),RING(Kind.BLOCK,9),RING_PANEL(Kind.PLATE,9),
    HEART(Kind.BLOCK,10),HEART_PANEL(Kind.PLATE,10),DOUBLE_SLAB(Kind.CUBE,-1),CIRCLE_C1(Kind.BLOCK,11),CIRCLE_C2(Kind.BLOCK,12),
    CIRCLE_C3(Kind.BLOCK,13),CIRCLE_C4(Kind.BLOCK,14),CIRCLE_C5(Kind.BLOCK,15),CIRCLE_C6(Kind.BLOCK,16),CIRCLE_C7(Kind.BLOCK,17),
    CIRCLE_C8(Kind.BLOCK,18),CIRCLE_C9(Kind.BLOCK,19),CIRCLE_C10(Kind.BLOCK,20),CIRCLE_C11(Kind.BLOCK,21),CIRCLE_C12(Kind.BLOCK,22),
    CIRCLE_C13(Kind.BLOCK,23),CIRCLE_C14(Kind.BLOCK,24),CIRCLE_C15(Kind.BLOCK,25),CIRCLE_PANEL_C1(Kind.PLATE,11),CIRCLE_PANEL_C2(Kind.PLATE,12),
    CIRCLE_PANEL_C3(Kind.PLATE,13),CIRCLE_PANEL_C4(Kind.PLATE,14),CIRCLE_PANEL_C5(Kind.PLATE,15),CIRCLE_PANEL_C6(Kind.PLATE,16),
    CIRCLE_PANEL_C7(Kind.PLATE,17),CIRCLE_PANEL_C8(Kind.PLATE,18),CIRCLE_PANEL_C9(Kind.PLATE,19),CIRCLE_PANEL_C10(Kind.PLATE,20),
    CIRCLE_PANEL_C11(Kind.PLATE,21),CIRCLE_PANEL_C12(Kind.PLATE,22),CIRCLE_PANEL_C13(Kind.PLATE,23),CIRCLE_PANEL_C14(Kind.PLATE,24),
    CIRCLE_PANEL_C15(Kind.PLATE,25);

    private enum Kind { CUBE, SLAB, PLATE, BLOCK }

    /** How far behind the block's face the front of a plate lies. */
    public static final double PLATE_DEPTH=15/16d;
    /** Corner bits of a joined round mirror, as seen from the front: set means square. */
    public static final int BOTTOM_LEFT=1,BOTTOM_RIGHT=2,TOP_LEFT=4,TOP_RIGHT=8;
    private static final int SIZE=16;
    private final Kind kind;
    /** Index of the outline in the generated masks; -1 for the forms without one. */
    private final int mask;

    MirrorShape(Kind kind,int mask) { this.kind=kind;this.mask=mask; }

    /** True for the plates, which fill only one side of the block. */
    public boolean flat() { return kind==Kind.PLATE; }

    /** True for the forms cut to an outline, plate or not. */
    public boolean outlined() { return kind==Kind.PLATE||kind==Kind.BLOCK; }

    /** True for the forms that fill their whole block. */
    public boolean cube() { return kind==Kind.CUBE; }

    /** True for the round mirror and what it becomes when it joins its neighbours. */
    public boolean round() { return mask==CIRCLE.mask||mask>HEART.mask; }

    /** Which corners of a round mirror are square; 0 for every other form. */
    public int corners() { return mask>HEART.mask?mask-HEART.mask:0; }

    /** The round mirror, block or plate as this one is, with these corners square. */
    public MirrorShape joined(int corners) {
        MirrorShape first=flat()?CIRCLE_PANEL_C1:CIRCLE_C1;
        return corners==0?(flat()?CIRCLE_PANEL:CIRCLE):values()[first.ordinal()+corners-1];
    }

    /** The outline's rows from the bottom up, one bit per pixel, as seen from the front. */
    private int[] rows(int rotation) { return rotated(MirrorShapeMasks.ROWS[mask],rotation); }

    /**
     * The outline as rectangles of pixels: left, bottom, right, top, as seen from the front. Equal runs on
     * neighbouring rows are joined, so a square outline is one rectangle.
     */
    public List<int[]> rectangles(int rotation) {
        int[] rows=rows(rotation);
        List<int[]> rectangles=new ArrayList<int[]>();
        List<int[]> growing=new ArrayList<int[]>();
        for(int row=0;row<=SIZE;row++) {
            List<int[]> next=new ArrayList<int[]>();
            int start=-1;
            for(int column=0;column<=SIZE&&row<SIZE;column++) {
                boolean filled=column<SIZE&&(rows[row]>>column&1)!=0;
                if(filled&&start<0)start=column;
                if(!filled&&start>=0) {
                    int[] run=null;
                    for(int[] candidate:growing)if(candidate[0]==start&&candidate[2]==column)run=candidate;
                    if(run==null) { run=new int[]{start,row,column,row};rectangles.add(run); }
                    run[3]=row+1;
                    next.add(run);
                    start=-1;
                }
            }
            growing=next;
        }
        return rectangles;
    }

    /**
     * The reflecting surfaces of one block.
     * @param nx,ny,nz the direction the mirror looks in; the block is attached on the opposite side
     * @param fx,fz the horizontal direction the block was placed facing, which orients a mirror on a floor or ceiling
     * @param rotation quarter turns of the outline, clockwise seen from the front
     */
    public List<MirrorSurface> surfaces(int x,int y,int z,int nx,int ny,int nz,int fx,int fz,int rotation) {
        if(cube())return Arrays.asList(MirrorSurface.faces(x,y,z));
        List<MirrorSurface> surfaces=new ArrayList<MirrorSurface>();
        if(this==SLAB) {
            surfaces.add(new MirrorSurface(x,y,z,nx,ny,nz,0,0,1,1,.5));
            surfaces.add(new MirrorSurface(x,y,z,-nx,-ny,-nz));
            for(MirrorSurface side:MirrorSurface.faces(x,y,z)) {
                if(side.nx*nx+side.ny*ny+side.nz*nz!=0)continue;
                // The half of this side that lies towards the attached side of the block.
                double[] u=MirrorSurface.axis(side.nx,side.ny,side.nz,true),v=MirrorSurface.axis(side.nx,side.ny,side.nz,false);
                double alongU=u[0]*nx+u[1]*ny+u[2]*nz,alongV=v[0]*nx+v[1]*ny+v[2]*nz;
                surfaces.add(new MirrorSurface(x,y,z,side.nx,side.ny,side.nz,alongU<0?.5:0,alongV<0?.5:0,alongU>0?.5:1,alongV>0?.5:1,0));
            }
            return surfaces;
        }
        double[] r=right(nx,ny,nz,fx,fz),t=up(nx,ny,nz,fx,fz),n={nx,ny,nz};
        boolean plate=flat();
        for(int[] box:rectangles(rotation)) {
            double left=box[0]/16d-.5,bottom=box[1]/16d-.5,right=box[2]/16d-.5,top=box[3]/16d-.5;
            surfaces.add(rectangle(x,y,z,n,plate?PLATE_DEPTH:0,span(r,left,right),span(t,bottom,top)));
            // A block of the outline also reflects on its back.
            if(!plate)surfaces.add(rectangle(x,y,z,new double[]{-nx,-ny,-nz},0,span(r,left,right),span(t,bottom,top)));
        }
        if(plate)return surfaces;
        // A side of the cube that the outline fills from end to end is a flat face, and reflects like one. Where
        // a curve only touches the side, the strip it leaves there stays plain.
        boolean fullLeft=true,fullRight=true,fullBottom=true,fullTop=true;
        for(int i=0;i<SIZE;i++) {
            fullLeft&=covers(rotation,0,i);fullRight&=covers(rotation,SIZE-1,i);
            fullBottom&=covers(rotation,i,0);fullTop&=covers(rotation,i,SIZE-1);
        }
        if(fullLeft)surfaces.add(new MirrorSurface(x,y,z,(int)-r[0],(int)-r[1],(int)-r[2]));
        if(fullRight)surfaces.add(new MirrorSurface(x,y,z,(int)r[0],(int)r[1],(int)r[2]));
        if(fullBottom)surfaces.add(new MirrorSurface(x,y,z,(int)-t[0],(int)-t[1],(int)-t[2]));
        if(fullTop)surfaces.add(new MirrorSurface(x,y,z,(int)t[0],(int)t[1],(int)t[2]));
        return surfaces;
    }

    /** True when the pixel belongs to the outline. */
    public boolean covers(int rotation,int column,int row) {
        return !outlined()||(rows(rotation)[row]>>column&1)!=0;
    }

    /**
     * Where the outline's left-to-right direction points in the world. It follows the block models: fixed by the
     * normal on a wall, and turned with the placement direction on a floor or ceiling.
     */
    public static double[] right(int nx,int ny,int nz,int fx,int fz) {
        return ny==0?new double[]{nz,0,-nx}:new double[]{fz,0,-fx};
    }

    /** Where the outline's bottom-to-top direction points in the world. */
    public static double[] up(int nx,int ny,int nz,int fx,int fz) {
        return ny==0?new double[]{0,1,0}:ny>0?new double[]{-fx,0,-fz}:new double[]{fx,0,fz};
    }

    /** The two ends of a side of a rectangle: a direction and how far along it each end lies from the block's middle. */
    private static double[][] span(double[] direction,double from,double to) {
        return new double[][]{{direction[0]*from,direction[1]*from,direction[2]*from},{direction[0]*to,direction[1]*to,direction[2]*to}};
    }

    /**
     * A surface from a rectangle given by its two sides, as offsets from the middle of the block. A surface's own
     * bounds run along axes that depend on its normal, so the rectangle is converted to them corner by corner.
     */
    private static MirrorSurface rectangle(int x,int y,int z,double[] normal,double depth,double[][] first,double[][] second) {
        int nx=(int)normal[0],ny=(int)normal[1],nz=(int)normal[2];
        double[] u=MirrorSurface.axis(nx,ny,nz,true),v=MirrorSurface.axis(nx,ny,nz,false);
        double a0=2,a1=-1,b0=2,b1=-1;
        for(int corner=0;corner<4;corner++) {
            double[] p=first[corner%2],q=second[corner/2];
            double px=p[0]+q[0],py=p[1]+q[1],pz=p[2]+q[2];
            // The axes are half a block long, so a dot product of .25 is the face's edge.
            double a=(px*u[0]+py*u[1]+pz*u[2])*2+.5,b=(px*v[0]+py*v[1]+pz*v[2])*2+.5;
            a0=Math.min(a0,a);a1=Math.max(a1,a);b0=Math.min(b0,b);b1=Math.max(b1,b);
        }
        return new MirrorSurface(x,y,z,nx,ny,nz,a0,b0,a1,b1,depth);
    }

    private static int[] rotated(int[] rows,int quarterTurns) {
        for(int turn=0;turn<(quarterTurns&3);turn++) {
            int[] next=new int[SIZE];
            for(int row=0;row<SIZE;row++)for(int column=0;column<SIZE;column++)
                if((rows[column]>>SIZE-1-row&1)!=0)next[row]|=1<<column;
            rows=next;
        }
        return rows;
    }
}
