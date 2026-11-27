package com.malidev.simplymirrorsplus.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Reflecting surfaces of a block carved into bits, 16 along each edge, some of which are mirror. Every bit
 * face that shows is found and neighbouring ones are joined into rectangles, so a block with one corner
 * chiselled away still costs a handful of surfaces and not thousands.
 */
public final class MirrorBits {
    public static final int SIZE=16;

    private MirrorBits() { }

    public static int index(int x,int y,int z) { return (x*SIZE+y)*SIZE+z; }

    /**
     * @param mirror which bits are mirror, by {@link #index}
     * @param solid which bits hold anything at all; a mirror face against one of these is hidden
     */
    public static List<MirrorSurface> surfaces(int x,int y,int z,boolean[] mirror,boolean[] solid) {
        List<MirrorSurface> surfaces=new ArrayList<MirrorSurface>();
        boolean[] shown=new boolean[SIZE*SIZE];
        int[] cell=new int[3];
        for(MirrorSurface face:MirrorSurface.faces(0,0,0)) {
            double[] u=MirrorSurface.axis(face.nx,face.ny,face.nz,true),v=MirrorSurface.axis(face.nx,face.ny,face.nz,false);
            // Along each block axis exactly one of these runs: the face's first edge, its second, or its depth.
            int[] first={sign(u[0]),sign(u[1]),sign(u[2])},second={sign(v[0]),sign(v[1]),sign(v[2])},inward={-face.nx,-face.ny,-face.nz};
            for(int depth=0;depth<SIZE;depth++) {
                boolean any=false;
                for(int b=0;b<SIZE;b++)for(int a=0;a<SIZE;a++) {
                    for(int axis=0;axis<3;axis++) {
                        int step=first[axis]!=0?first[axis]:second[axis]!=0?second[axis]:inward[axis];
                        int along=first[axis]!=0?a:second[axis]!=0?b:depth;
                        cell[axis]=step>0?along:SIZE-1-along;
                    }
                    boolean show=mirror[index(cell[0],cell[1],cell[2])];
                    if(show&&depth>0)show=!solid[index(cell[0]+face.nx,cell[1]+face.ny,cell[2]+face.nz)];
                    shown[b*SIZE+a]=show;
                    any|=show;
                }
                if(!any)continue;
                for(int b=0;b<SIZE;b++)for(int a=0;a<SIZE;a++) {
                    if(!shown[b*SIZE+a])continue;
                    int a1=a;
                    while(a1<SIZE&&shown[b*SIZE+a1])a1++;
                    int b1=b+1;
                    rows:while(b1<SIZE) {
                        for(int i=a;i<a1;i++)if(!shown[b1*SIZE+i])break rows;
                        b1++;
                    }
                    for(int j=b;j<b1;j++)for(int i=a;i<a1;i++)shown[j*SIZE+i]=false;
                    surfaces.add(new MirrorSurface(x,y,z,face.nx,face.ny,face.nz,a/(double)SIZE,b/(double)SIZE,a1/(double)SIZE,b1/(double)SIZE,depth/(double)SIZE));
                }
            }
        }
        return surfaces;
    }

    private static int sign(double value) { return value>0?1:value<0?-1:0; }
}
