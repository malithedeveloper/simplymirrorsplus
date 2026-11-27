package com.malidev.simplymirrorsplus.math;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The frame of a mirror: a band along the outer edge of the surfaces that share a plane, however many blocks
 * they span and whatever their outline. A hole, as in a ring, gets no band, or a thin mirror would be all frame. It is worked out on the grid of texture pixels, 16 to a block, so the
 * band follows a round mirror's steps exactly and is textured pixel for pixel like the block it is made of.
 */
public final class MirrorFrameMesh {
    /** Width of the band in pixels. */
    public static final int WIDTH=2;
    private static final int PIXELS=16;

    /** Per rectangle: four corners as x, y, z, then the two texture corners in pixels of the 16 by 16 tile. */
    public final float[] quads;
    public final int count;

    private MirrorFrameMesh(float[] quads,int count) { this.quads=quads;this.count=count; }

    /** @param surfaces coplanar surfaces of the mirrors that share one frame */
    public static MirrorFrameMesh build(List<MirrorSurface> surfaces) {
        if(surfaces.isEmpty())return new MirrorFrameMesh(new float[0],0);
        MirrorSurface first=surfaces.get(0);
        double[] u=MirrorSurface.axis(first.nx,first.ny,first.nz,true),v=MirrorSurface.axis(first.nx,first.ny,first.nz,false);
        // The axes are half a block long; doubled they are the unit directions of the pixel grid.
        double ux=u[0]*2,uy=u[1]*2,uz=u[2]*2,vx=v[0]*2,vy=v[1]*2,vz=v[2]*2;
        Set<Long> filled=new HashSet<Long>();
        double[] corners=new double[12];
        int minU=Integer.MAX_VALUE,minV=Integer.MAX_VALUE,maxU=Integer.MIN_VALUE,maxV=Integer.MIN_VALUE;
        for(MirrorSurface surface:surfaces) {
            surface.writeCorners(corners);
            int u0=Integer.MAX_VALUE,v0=Integer.MAX_VALUE,u1=Integer.MIN_VALUE,v1=Integer.MIN_VALUE;
            for(int corner=0;corner<12;corner+=3) {
                int pu=(int)Math.round((corners[corner]*ux+corners[corner+1]*uy+corners[corner+2]*uz)*PIXELS);
                int pv=(int)Math.round((corners[corner]*vx+corners[corner+1]*vy+corners[corner+2]*vz)*PIXELS);
                u0=Math.min(u0,pu);u1=Math.max(u1,pu);v0=Math.min(v0,pv);v1=Math.max(v1,pv);
            }
            for(int pv=v0;pv<v1;pv++)for(int pu=u0;pu<u1;pu++)filled.add(key(pu,pv));
            minU=Math.min(minU,u0);maxU=Math.max(maxU,u1);minV=Math.min(minV,v0);maxV=Math.max(maxV,v1);
        }
        // Everything that can be reached from beyond the bounds without crossing the mirror is outside it.
        Set<Long> outside=new HashSet<Long>();
        java.util.ArrayDeque<int[]> open=new java.util.ArrayDeque<int[]>();
        int lowU=minU-WIDTH-1,lowV=minV-WIDTH-1,highU=maxU+WIDTH,highV=maxV+WIDTH;
        open.add(new int[]{lowU,lowV});
        outside.add(key(lowU,lowV));
        while(!open.isEmpty()) {
            int[] cell=open.poll();
            for(int side=0;side<4;side++) {
                int pu=cell[0]+(side==0?1:side==1?-1:0),pv=cell[1]+(side==2?1:side==3?-1:0);
                if(pu<lowU||pu>highU||pv<lowV||pv>highV||filled.contains(key(pu,pv))||!outside.add(key(pu,pv)))continue;
                open.add(new int[]{pu,pv});
            }
        }
        // The plane's own coordinate, a little in front of the mirror so the band covers the reflection's edge.
        double plane=first.nx*first.cx+first.ny*first.cy+first.nz*first.cz+.004;
        float[] quads=new float[Math.max(16,filled.size()/4)*16];
        int count=0;
        for(int pv=minV;pv<maxV;pv++) {
            int start=Integer.MIN_VALUE;
            for(int pu=minU;pu<=maxU;pu++) {
                boolean band=pu<maxU&&filled.contains(key(pu,pv))&&nearEdge(outside,pu,pv);
                // A run also ends at a block boundary, where the texture starts over.
                if(start!=Integer.MIN_VALUE&&(!band||Math.floorMod(pu,PIXELS)==0)) {
                    if(count*16+16>quads.length)quads=java.util.Arrays.copyOf(quads,quads.length*2);
                    int at=count++*16;
                    float tu0=Math.floorMod(start,PIXELS),tu1=tu0+(pu-start),tv0=Math.floorMod(pv,PIXELS),tv1=tv0+1;
                    int[][] corner={{start,pv},{pu,pv},{pu,pv+1},{start,pv+1}};
                    for(int i=0;i<4;i++) {
                        double a=corner[i][0]/(double)PIXELS,b=corner[i][1]/(double)PIXELS;
                        quads[at+i*3]=(float)(a*ux+b*vx+plane*first.nx);
                        quads[at+i*3+1]=(float)(a*uy+b*vy+plane*first.ny);
                        quads[at+i*3+2]=(float)(a*uz+b*vz+plane*first.nz);
                    }
                    quads[at+12]=tu0;quads[at+13]=tv0;quads[at+14]=tu1;quads[at+15]=tv1;
                    start=Integer.MIN_VALUE;
                }
                if(band&&start==Integer.MIN_VALUE)start=pu;
            }
        }
        return new MirrorFrameMesh(quads,count);
    }

    private static boolean nearEdge(Set<Long> outside,int pu,int pv) {
        for(int dv=-WIDTH;dv<=WIDTH;dv++)for(int du=-WIDTH;du<=WIDTH;du++)if(outside.contains(key(pu+du,pv+dv)))return true;
        return false;
    }

    private static long key(int pu,int pv) { return (long)pu<<32^(pv&0xFFFFFFFFL); }
}
