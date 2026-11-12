package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.math.MirrorFaceSelector;
import com.malidev.simplymirrorsplus.math.MirrorMatrices;
import com.malidev.simplymirrorsplus.math.MirrorRefreshClock;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.malidev.simplymirrorsplus.math.MirrorTemporalHistory;
import com.malidev.simplymirrorsplus.math.ReflectionTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Version-independent mirror pipeline. It picks the visible mirror planes, keeps one image per plane,
 * schedules refreshes, recurses into mirrors seen inside a reflection and asks for the composites.
 * Each Minecraft version supplies a {@link Platform} that renders the world and draws the composite.
 */
public final class MirrorEngine<T> {
    public static final int MODE_NONE=-1,MODE_NORMAL=0,MODE_PLAYERS=1,MODE_OFF=2;
    static final int MAX_PLANES=6;
    static final int MAX_CACHED_PLANES=6;
    /** Spreads refreshes over frames so a room full of mirrors cannot spike one frame. */
    static final int MAX_PLANE_UPDATES_PER_FRAME=2;
    /** 1 = plain reflection, 2 = a mirror visible inside a reflection also reflects. */
    static final int MAX_DEPTH=2;
    static final int MAX_SPARES=3;
    /** Images of mirrors that stay out of view this long are freed. */
    static final long UNUSED_NANOS=5_000_000_000L;
    /** Image sizes snap to this step so a mirror moving across the screen does not reallocate every frame. */
    static final int SIZE_STEP=32;
    static final int MIN_SIZE=16;

    public static long completedPasses;

    /** What a Minecraft version provides. {@code T} is that version's render target type. */
    public interface Platform<T> {
        T createTarget(int width,int height);
        void destroyTarget(T target);
        int width(T target);
        int height(T target);
        /** Mirror faces around the eye; exposure and mode are checked separately every frame. */
        List<MirrorSurface> findSurfaces(double x,double y,double z);
        /** One of the MODE_ constants for the mirror behind this face, MODE_NONE when it is gone or covered. */
        int faceMode(MirrorSurface face);
        boolean shadersActive();
        boolean depthZeroToOne();
        /** True when near maps to the top of the depth range (Minecraft 26.2+). */
        default boolean depthReversed() { return false; }
        /** Renders {@code capture.self} into {@code capture.into}, runs {@code capture.nested} with it still bound, then restores the parent's state. */
        void capture(Capture<T> capture);
        /** Draws {@code faces} into {@code into} (null means the main target) sampling {@code source} through {@code sourceViewProjection}. */
        void composite(T into,T source,float[] viewProjection,double[] eye,float[] sourceViewProjection,double[] sourceEye,List<MirrorSurface> faces);
    }

    /** One rendered view: the player's camera or a reflection of it. */
    public static final class Pass {
        /** Version-specific camera object, set by the platform when it creates the reflected camera. */
        public Object camera;
        /** Where this view is drawn: the main target for the player's camera, the capture image for a reflection. */
        public Object target;
        public final double[] eye;
        public final float[] view,projection;
        public final int depth;
        public Pass(Object camera,double[] eye,float[] view,float[] projection,int depth) {
            this.camera=camera;this.eye=eye;this.view=view;this.projection=projection;this.depth=depth;
        }
        public float[] viewProjection() { return MirrorMatrices.multiply(projection,view); }
    }

    public static final class Capture<T> {
        public final Pass parent,self;
        public final MirrorSurface surface;
        public final T into;
        public final boolean playersOnly,shaders;
        /** The player's projection without the mirror's clip plane; use it for frustum culling. */
        public final float[] perspective;
        public final MirrorTemporalHistory.Pose prior;
        /** Mirrors seen in this reflection; null when they are not rendered (shaders, Players Only). */
        public final Runnable nested;
        Capture(Pass parent,Pass self,MirrorSurface surface,T into,boolean playersOnly,boolean shaders,float[] perspective,MirrorTemporalHistory.Pose prior,Runnable nested) {
            this.parent=parent;this.self=self;this.surface=surface;this.into=into;this.playersOnly=playersOnly;
            this.shaders=shaders;this.perspective=perspective;this.prior=prior;this.nested=nested;
        }
        /** Mirror centre relative to the reflected eye, as ShaderReflection expects it. */
        public double offsetX() { return surface.cx-self.eye[0]; }
        public double offsetY() { return surface.cy-self.eye[1]; }
        public double offsetZ() { return surface.cz-self.eye[2]; }
    }

    /** Screen area a reflection needs, in normalized device coordinates, and the image size for it. */
    static final class Crop {
        final double x0,y0,x1,y1;
        final int width,height;
        Crop(double x0,double y0,double x1,double y1,int width,int height) {
            this.x0=x0;this.y0=y0;this.x1=x1;this.y1=y1;this.width=width;this.height=height;
        }
        /** Maps the area onto the whole image: x' = (2x - (x0+x1)w) / (x1-x0), same for y. */
        float[] apply(float[] matrix) {
            double sx=2/(x1-x0),sy=2/(y1-y0),tx=-(x1+x0)/(x1-x0),ty=-(y1+y0)/(y1-y0);
            float[] out=matrix.clone();
            for(int column=0;column<4;column++) {
                int at=column*4;
                out[at]=(float)(sx*matrix[at]+tx*matrix[at+3]);
                out[at+1]=(float)(sy*matrix[at+1]+ty*matrix[at+3]);
            }
            return out;
        }
    }

    private static final class Slot<T> {
        final MirrorRefreshClock clock=new MirrorRefreshClock();
        T target;
        List<MirrorSurface> faces;
        int mode;
        int seenFrame=-2;
        long lastSeen;
        float[] capturedViewProjection;
        double[] capturedEye;
    }

    private static final class Spare<T> {
        final T target;
        final long since;
        Spare(T target,long since) { this.target=target;this.since=since; }
    }

    private static final class Group {
        final String key;
        final int mode;
        final List<MirrorSurface> faces=new ArrayList<MirrorSurface>();
        Group(String key,int mode) { this.key=key;this.mode=mode; }
    }

    private static final class Frame {
        final float[] perspective;
        final MirrorTemporalHistory.Frame prior;
        final boolean shaders;
        Frame(float[] perspective,MirrorTemporalHistory.Frame prior,boolean shaders) {
            this.perspective=perspective;this.prior=prior;this.shaders=shaders;
        }
    }

    private static final Logger LOGGER=Logger.getLogger("simplymirrorsplus");
    private final Platform<T> platform;
    private final MirrorTemporalHistory temporal=new MirrorTemporalHistory();
    private final Map<String,Slot<T>> slots=new LinkedHashMap<String,Slot<T>>();
    private final List<Spare<T>> spares=new ArrayList<Spare<T>>();
    private final List<T> nested=new ArrayList<T>();
    private final double[] corners=new double[12];
    private List<MirrorSurface> cached=new ArrayList<MirrorSurface>();
    private Object lastLevel;
    private int frame;
    private boolean failed;

    public MirrorEngine(Platform<T> platform) {
        this.platform=platform;
        for(int i=1;i<MAX_DEPTH;i++)nested.add(null);
    }

    /**
     * Renders and composites every visible mirror for one frame.
     * @param root the player's camera, with the projection used to draw the frame
     * @param level any object that changes identity when the world changes
     */
    public void render(Pass root,int mainWidth,int mainHeight,Object level,long now) {
        if(!MirrorConfig.enabled) { releaseTargets();return; }
        if(level!=lastLevel) { release();lastLevel=level; }
        int thisFrame=frame++;
        final double[] eye=root.eye;
        if(thisFrame%20==0)cached=platform.findSurfaces(eye[0],eye[1],eye[2]);
        MirrorTemporalHistory.Frame prior=temporal.advance(root.view,root.projection,eye[0],eye[1],eye[2]);
        releaseUnused(now);
        if(cached.isEmpty()||failed)return;
        cached.sort(new Comparator<MirrorSurface>() {
            @Override public int compare(MirrorSurface a,MirrorSurface b) { return Double.compare(distance(a,eye),distance(b,eye)); }
        });
        boolean shaders=platform.shadersActive();
        Frame context=new Frame(root.projection,prior,shaders);
        float[] vp=root.viewProjection();
        List<Slot<T>> planes=new ArrayList<Slot<T>>();
        for(Group group:visiblePlanes(root,null)) {
            if(planes.size()==MAX_PLANES)break;
            Slot<T> slot=slots.get(group.key);
            if(slot==null) { slot=new Slot<T>();slots.put(group.key,slot); }
            slot.faces=group.faces;
            slot.mode=group.mode;
            if(slot.seenFrame!=thisFrame-1)slot.clock.reset();
            slot.seenFrame=thisFrame;
            slot.lastSeen=now;
            planes.add(slot);
        }
        evictExtraSlots();
        planes.sort(new Comparator<Slot<T>>() {
            @Override public int compare(Slot<T> a,Slot<T> b) { return Long.compare(a.clock.due(),b.clock.due()); }
        });
        // Iris sizes its gbuffers from the depth target, so with shaders the image keeps the screen size.
        int percent=shaders?100:MirrorConfig.resolutionPercent;
        int baseWidth=Math.max(MIN_SIZE,mainWidth*percent/100),baseHeight=Math.max(MIN_SIZE,mainHeight*percent/100);
        long interval=1_000_000_000L/MirrorConfig.refreshRate;
        try {
            int updates=0;
            for(Slot<T> slot:planes) {
                if(updates==MAX_PLANE_UPDATES_PER_FRAME||!slot.clock.isDue(now))continue;
                Crop crop=crop(slot.faces,vp,eye,baseWidth,baseHeight,shaders);
                T image=takeSpare(crop.width,crop.height);
                capture(context,root,slot.faces.get(0),image,crop,slot.mode==MODE_PLAYERS);
                if(slot.target!=null)giveSpare(slot.target,now);
                slot.target=image;
                slot.capturedViewProjection=crop.apply(vp);
                slot.capturedEye=eye.clone();
                slot.clock.rendered(now,interval);
                updates++;
                completedPasses++;
            }
            for(Slot<T> slot:planes)
                if(slot.target!=null)platform.composite(null,slot.target,vp,eye,slot.capturedViewProjection,slot.capturedEye,slot.faces);
        } catch(RuntimeException exception) {
            failed=true;
            LOGGER.log(Level.SEVERE,"Mirror reflection disabled for this world after a rendering error",exception);
        }
    }

    private void capture(Frame frame,Pass parent,MirrorSurface surface,T into,Crop crop,boolean playersOnly) {
        double[] eye=surface.plane().reflectPoint(parent.eye[0],parent.eye[1],parent.eye[2]);
        float[] view=MirrorMatrices.multiply(parent.view,ReflectionTransform.matrix(surface.nx,surface.ny,surface.nz));
        float[] projection=frame.shaders?frame.perspective:MirrorMatrices.oblique(frame.perspective,view,surface.nx,surface.ny,surface.nz,
                surface.cx-eye[0],surface.cy-eye[1],surface.cz-eye[2],platform.depthZeroToOne(),platform.depthReversed());
        final Pass self=new Pass(null,eye,view,crop.apply(projection),parent.depth+1);
        self.target=into;
        final MirrorSurface parentSurface=surface;
        final T target=into;
        final Frame context=frame;
        // ponytail: Iris reflection state is single-level, so shader packs stop at one bounce.
        Runnable nestedMirrors=frame.shaders||playersOnly?null:new Runnable() {
            @Override public void run() { renderMirrorsInReflection(context,self,parentSurface,target); }
        };
        platform.capture(new Capture<T>(parent,self,surface,into,playersOnly,frame.shaders,frame.perspective,frame.prior.reflect(surface),nestedMirrors));
    }

    /**
     * Without this a mirror seen inside a reflection shows its block texture. The nearest plane gets a real
     * bounce; the rest, and the last bounce, reuse images that already exist so they cost no world render.
     */
    private void renderMirrorsInReflection(Frame frame,Pass self,MirrorSurface parentSurface,T into) {
        float[] vp=self.viewProjection();
        boolean realBounce=self.depth<MAX_DEPTH;
        for(Group group:visiblePlanes(self,parentSurface)) {
            T source;
            float[] sourceViewProjection;
            double[] sourceEye=self.eye;
            if(realBounce) {
                realBounce=false;
                int index=self.depth-1;
                Crop crop=crop(group.faces,vp,self.eye,platform.width(into),platform.height(into),false);
                T image=nested.get(index);
                if(image==null||platform.width(image)!=crop.width||platform.height(image)!=crop.height) {
                    if(image!=null)platform.destroyTarget(image);
                    image=platform.createTarget(crop.width,crop.height);
                    nested.set(index,image);
                }
                capture(frame,self,group.faces.get(0),image,crop,group.mode==MODE_PLAYERS);
                source=image;
                sourceViewProjection=crop.apply(vp);
            } else {
                Slot<T> slot=slots.get(group.key);
                if(slot==null||slot.target==null||slot.target==into)continue;
                // ponytail: this image was taken from the main camera, so its perspective is approximate at this depth.
                source=slot.target;
                sourceViewProjection=slot.capturedViewProjection;
                sourceEye=slot.capturedEye;
            }
            platform.composite(into,source,vp,self.eye,sourceViewProjection,sourceEye,group.faces);
        }
    }

    /** Visible planes nearest first, split by mode; {@code behind} limits them to what lies in front of that mirror. */
    private List<Group> visiblePlanes(Pass view,final MirrorSurface behind) {
        final Map<MirrorSurface,Integer> modes=new IdentityHashMap<MirrorSurface,Integer>();
        List<MirrorSurface> selected=MirrorFaceSelector.select(cached,view.viewProjection(),view.eye[0],view.eye[1],view.eye[2],
                MirrorFaceSelector.MAX_VISIBLE_FACES,new java.util.function.Predicate<MirrorSurface>() {
                    @Override public boolean test(MirrorSurface face) {
                        // Geometry behind the parent mirror was clipped away, so nothing there may be composited.
                        if(behind!=null&&!behind.frontFacing(face.cx,face.cy,face.cz,.01))return false;
                        int mode=platform.faceMode(face);
                        if(mode==MODE_NORMAL&&MirrorConfig.playersOnly)mode=MODE_PLAYERS;
                        modes.put(face,mode);
                        return mode==MODE_NORMAL||mode==MODE_PLAYERS;
                    }
                });
        Map<String,Group> groups=new LinkedHashMap<String,Group>();
        for(MirrorSurface face:selected) {
            int mode=modes.get(face);
            String key=face.planeKey()+"/"+mode;
            Group group=groups.get(key);
            if(group==null) { group=new Group(key,mode);groups.put(key,group); }
            group.faces.add(face);
        }
        return new ArrayList<Group>(groups.values());
    }

    /** Smallest screen area holding the faces, padded so a refresh-limited image still covers it while the camera moves. */
    Crop crop(List<MirrorSurface> faces,float[] viewProjection,double[] eye,int baseWidth,int baseHeight,boolean fullScreen) {
        double x0=1,y0=1,x1=-1,y1=-1;
        boolean whole=fullScreen;
        for(int i=0;i<faces.size()&&!whole;i++) {
            faces.get(i).writeCorners(corners);
            for(int c=0;c<12;c+=3) {
                double[] clip=MirrorMatrices.transform(viewProjection,corners[c]-eye[0],corners[c+1]-eye[1],corners[c+2]-eye[2],1);
                // A corner behind the camera has no screen position; the whole screen is the only safe bound.
                if(clip[3]<=1e-3) { whole=true;break; }
                double x=clip[0]/clip[3],y=clip[1]/clip[3];
                x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
            }
        }
        if(!whole) {
            double padX=.05+(x1-x0)*.1,padY=.05+(y1-y0)*.1;
            x0=Math.max(-1,x0-padX);x1=Math.min(1,x1+padX);y0=Math.max(-1,y0-padY);y1=Math.min(1,y1+padY);
            if(x1-x0<.02||y1-y0<.02)whole=true;
        }
        if(whole) { x0=-1;y0=-1;x1=1;y1=1; }
        int width=fullScreen?baseWidth:snap(baseWidth*(x1-x0)/2,baseWidth);
        int height=fullScreen?baseHeight:snap(baseHeight*(y1-y0)/2,baseHeight);
        return new Crop(x0,y0,x1,y1,width,height);
    }

    private static int snap(double size,int limit) {
        int stepped=(int)Math.ceil(Math.max(MIN_SIZE,size)/SIZE_STEP)*SIZE_STEP;
        return Math.max(MIN_SIZE,Math.min(limit,stepped));
    }

    private T takeSpare(int width,int height) {
        for(Iterator<Spare<T>> it=spares.iterator();it.hasNext();) {
            Spare<T> spare=it.next();
            if(platform.width(spare.target)==width&&platform.height(spare.target)==height) { it.remove();return spare.target; }
        }
        return platform.createTarget(width,height);
    }

    private void giveSpare(T target,long now) {
        if(spares.size()>=MAX_SPARES)platform.destroyTarget(spares.remove(0).target);
        spares.add(new Spare<T>(target,now));
    }

    private void releaseUnused(long now) {
        for(Iterator<Slot<T>> it=slots.values().iterator();it.hasNext();) {
            Slot<T> slot=it.next();
            if(now-slot.lastSeen>UNUSED_NANOS) { if(slot.target!=null)platform.destroyTarget(slot.target);it.remove(); }
        }
        for(Iterator<Spare<T>> it=spares.iterator();it.hasNext();) {
            Spare<T> spare=it.next();
            if(now-spare.since>UNUSED_NANOS) { platform.destroyTarget(spare.target);it.remove(); }
        }
    }

    private void evictExtraSlots() {
        while(slots.size()>MAX_CACHED_PLANES) {
            Map.Entry<String,Slot<T>> oldest=null;
            for(Map.Entry<String,Slot<T>> entry:slots.entrySet())
                if(oldest==null||entry.getValue().lastSeen<oldest.getValue().lastSeen)oldest=entry;
            if(oldest.getValue().target!=null)platform.destroyTarget(oldest.getValue().target);
            slots.remove(oldest.getKey());
        }
    }

    /** Number of render targets currently held, for tests and memory checks. */
    int heldTargets() {
        int count=spares.size();
        for(Slot<T> slot:slots.values())if(slot.target!=null)count++;
        for(T image:nested)if(image!=null)count++;
        return count;
    }

    public void releaseTargets() {
        for(Slot<T> slot:slots.values())if(slot.target!=null)platform.destroyTarget(slot.target);
        slots.clear();
        for(Spare<T> spare:spares)platform.destroyTarget(spare.target);
        spares.clear();
        for(int i=0;i<nested.size();i++) { if(nested.get(i)!=null)platform.destroyTarget(nested.get(i));nested.set(i,null); }
    }

    public void release() {
        releaseTargets();
        cached=new ArrayList<MirrorSurface>();
        lastLevel=null;
        frame=0;
        failed=false;
        temporal.reset();
    }

    private static double distance(MirrorSurface s,double[] eye) {
        double x=s.cx-eye[0],y=s.cy-eye[1],z=s.cz-eye[2];
        return x*x+y*y+z*z;
    }
}
