package com.malidev.simplymirrorsplus.math;

/**
 * One reflecting rectangle on a mirror block: a whole face of the cube, or part of one that may also lie
 * inside the block, as the front of a slab or of a shaped plate does.
 */
public final class MirrorSurface implements MirrorGeometry {
    public final int x,y,z,nx,ny,nz;
    public final double cx,cy,cz;
    /** Bounds on the face, 0 to 1 along the two axes of {@link #axis}, and the distance behind the block's face. */
    public final double a0,b0,a1,b1,depth;
    /** True for a whole face on the outside of the block, which a neighbouring mirror can cover and patches can merge. */
    public final boolean whole;
    /** A point on the plane this surface reflects in. It is the surface's own centre unless the surface was {@link #flattened}. */
    public final double px,py,pz;
    private final String planeKey;
    public MirrorSurface(int x,int y,int z,int nx,int nz) {
        this(x,y,z,nx,0,nz);
    }
    public MirrorSurface(int x,int y,int z,int nx,int ny,int nz) {
        this(x,y,z,nx,ny,nz,0,0,1,1,0);
    }
    public MirrorSurface(int x,int y,int z,int nx,int ny,int nz,double a0,double b0,double a1,double b1,double depth) {
        this(x,y,z,nx,ny,nz,a0,b0,a1,b1,depth,false);
    }
    private MirrorSurface(int x,int y,int z,int nx,int ny,int nz,double a0,double b0,double a1,double b1,double depth,boolean flattened) {
        if(Math.abs(nx)+Math.abs(ny)+Math.abs(nz)!=1)throw new IllegalArgumentException("Expected a cube-face normal");
        this.x=x;this.y=y;this.z=z;this.nx=nx;this.ny=ny;this.nz=nz;
        this.a0=a0;this.b0=b0;this.a1=a1;this.b1=b1;this.depth=depth;
        whole=a0==0&&b0==0&&a1==1&&b1==1&&depth==0;
        double[] u=axis(nx,ny,nz,true),v=axis(nx,ny,nz,false);
        double a=a0+a1-1,b=b0+b1-1,out=.5-depth;
        cx=x+.5+nx*out+a*u[0]+b*v[0];cy=y+.5+ny*out+a*u[1]+b*v[1];cz=z+.5+nz*out+a*u[2]+b*v[2];
        double behind=flattened?depth:0;
        px=cx+nx*behind;py=cy+ny*behind;pz=cz+nz*behind;
        planeKey=nx+":"+ny+":"+nz+":"+Math.round((nx*px+ny*py+nz*pz)*16);
    }
    /**
     * The same rectangle, reflecting in the plane of its block's outer face instead of its own. Surfaces at many
     * depths, as chiselled bits have, then share one reflection; each is a little off, by twice its depth.
     */
    public MirrorSurface flattened() { return depth==0?this:new MirrorSurface(x,y,z,nx,ny,nz,a0,b0,a1,b1,depth,true); }
    /** Half of the first (or second) edge direction of a face with this normal; bounds run along these two. */
    public static double[] axis(int nx,int ny,int nz,boolean first) {
        if(first)return new double[]{ny==0?-nz*.5:.5,0,ny==0?nx*.5:0};
        return new double[]{0,ny==0?.5:0,ny*.5};
    }
    public static MirrorSurface[] faces(int x,int y,int z) {
        return new MirrorSurface[]{new MirrorSurface(x,y,z,0,0,-1),new MirrorSurface(x,y,z,0,0,1),
            new MirrorSurface(x,y,z,-1,0,0),new MirrorSurface(x,y,z,1,0,0),
            new MirrorSurface(x,y,z,0,1,0),new MirrorSurface(x,y,z,0,-1,0)};
    }
    public MirrorPlane plane() { return new MirrorPlane(px,py,pz,nx,ny,nz); }
    public String planeKey() { return planeKey; }
    /** Axis normals are already unit length; skip the temporary MirrorPlane allocation. */
    public boolean frontFacing(double eyeX,double eyeY,double eyeZ,double epsilon) {
        return (eyeX-cx)*nx+(eyeY-cy)*ny+(eyeZ-cz)*nz>epsilon;
    }
    public double[][] corners() {
        double[] c=new double[12];writeCorners(c);
        return new double[][]{{c[0],c[1],c[2]},{c[3],c[4],c[5]},
                {c[6],c[7],c[8]},{c[9],c[10],c[11]}};
    }
    private static void corner(double[] c,int i,double x,double y,double z) {
        int n=i*3;c[n]=x;c[n+1]=y;c[n+2]=z;
    }
    @Override public void writeCorners(double[] out) {
        double[] u=axis(nx,ny,nz,true),v=axis(nx,ny,nz,false);
        double fx=x+.5+nx*(.5-depth+.001),fy=y+.5+ny*(.5-depth+.001),fz=z+.5+nz*(.5-depth+.001);
        double ua0=2*a0-1,ua1=2*a1-1,vb0=2*b0-1,vb1=2*b1-1;
        corner(out,0,fx+ua1*u[0]+vb0*v[0],fy+ua1*u[1]+vb0*v[1],fz+ua1*u[2]+vb0*v[2]);
        corner(out,1,fx+ua0*u[0]+vb0*v[0],fy+ua0*u[1]+vb0*v[1],fz+ua0*u[2]+vb0*v[2]);
        corner(out,2,fx+ua0*u[0]+vb1*v[0],fy+ua0*u[1]+vb1*v[1],fz+ua0*u[2]+vb1*v[2]);
        corner(out,3,fx+ua1*u[0]+vb1*v[0],fy+ua1*u[1]+vb1*v[1],fz+ua1*u[2]+vb1*v[2]);
    }
}
