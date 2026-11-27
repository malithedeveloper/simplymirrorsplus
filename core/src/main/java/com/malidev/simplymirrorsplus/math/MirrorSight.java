package com.malidev.simplymirrorsplus.math;

import java.util.List;

/**
 * Tells whether the player can see a mirror at all, or whether solid blocks stand in the way. A mirror behind a
 * wall is inside the view like any other, and would be reflected for nobody.
 */
public final class MirrorSight {
    /** The blocks of the world that nothing can be seen through. */
    public interface Solid {
        boolean at(int x,int y,int z);
    }

    /** Faces looked at per mirror plane, spread over it; a larger mirror is not looked at more closely. */
    static final int FACES=12;
    /** A sight line ends this far in front of the face, so the mirror's own block is not in its way. */
    private static final double BEFORE=.02;

    private MirrorSight() { }

    /**
     * True when at least one of a spread of points on the faces can be seen from the eye. Only a mirror with
     * every one of them hidden counts as out of sight, so a mirror seen through a gap still reflects.
     */
    public static boolean any(List<MirrorSurface> faces,double eyeX,double eyeY,double eyeZ,Solid solid) {
        double[] corners=new double[12];
        int step=Math.max(1,faces.size()/FACES);
        for(int i=0;i<faces.size();i+=step) {
            MirrorSurface face=faces.get(i);
            face.writeCorners(corners);
            double nx=face.nx*BEFORE,ny=face.ny*BEFORE,nz=face.nz*BEFORE;
            if(reaches(eyeX,eyeY,eyeZ,face.cx+nx,face.cy+ny,face.cz+nz,solid))return true;
            for(int corner=0;corner<12;corner+=3) {
                // A little inside the corner, where a neighbouring block cannot hide the point by touching it.
                double x=corners[corner]+(face.cx-corners[corner])*.15+nx,y=corners[corner+1]+(face.cy-corners[corner+1])*.15+ny,z=corners[corner+2]+(face.cz-corners[corner+2])*.15+nz;
                if(reaches(eyeX,eyeY,eyeZ,x,y,z,solid))return true;
            }
        }
        return false;
    }

    /** Walks the blocks along the line from one point to another; false as soon as one of them is solid. */
    public static boolean reaches(double fromX,double fromY,double fromZ,double toX,double toY,double toZ,Solid solid) {
        int x=(int)Math.floor(fromX),y=(int)Math.floor(fromY),z=(int)Math.floor(fromZ);
        int endX=(int)Math.floor(toX),endY=(int)Math.floor(toY),endZ=(int)Math.floor(toZ);
        double dx=toX-fromX,dy=toY-fromY,dz=toZ-fromZ;
        int stepX=dx>0?1:-1,stepY=dy>0?1:-1,stepZ=dz>0?1:-1;
        // How far along the line, 0 to 1, the next block boundary lies on each axis, and the distance between two of them.
        double nextX=dx==0?Double.MAX_VALUE:((dx>0?x+1:x)-fromX)/dx,nextY=dy==0?Double.MAX_VALUE:((dy>0?y+1:y)-fromY)/dy,nextZ=dz==0?Double.MAX_VALUE:((dz>0?z+1:z)-fromZ)/dz;
        double deltaX=dx==0?Double.MAX_VALUE:Math.abs(1/dx),deltaY=dy==0?Double.MAX_VALUE:Math.abs(1/dy),deltaZ=dz==0?Double.MAX_VALUE:Math.abs(1/dz);
        // The block the eye is in does not count: the camera may sit inside one in third person.
        for(int guard=0;guard<512&&(x!=endX||y!=endY||z!=endZ);guard++) {
            double entered;
            if(nextX<=nextY&&nextX<=nextZ) { entered=nextX;x+=stepX;nextX+=deltaX; }
            else if(nextY<=nextZ) { entered=nextY;y+=stepY;nextY+=deltaY; }
            else { entered=nextZ;z+=stepZ;nextZ+=deltaZ; }
            // Past the end of the line: rounding stepped around the last block, and nothing was in the way.
            if(entered>1)return true;
            if(solid.at(x,y,z))return false;
        }
        return true;
    }
}
