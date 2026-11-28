package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.math.MirrorFaceSelector;
import com.malidev.simplymirrorsplus.math.MirrorMatrices;
import com.malidev.simplymirrorsplus.math.MirrorSurface;
import com.malidev.simplymirrorsplus.math.MirrorTemporalHistory;
import com.malidev.simplymirrorsplus.math.ReflectionTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Version-independent mirror pipeline. It picks the visible mirror planes, keeps one image per plane,
 * redraws the nearest ones every frame and the rest in turn, recurses into mirrors seen inside a reflection
 * and asks for the composites. How many planes, bounces and pixels that is comes from {@link MirrorConfig#performance}.
 * Each Minecraft version supplies a {@link Platform} that renders the world and draws the composite.
 */
public final class MirrorEngine<T> {
    public static final int MODE_NONE=-1,MODE_NORMAL=0,MODE_PLAYERS=1,MODE_OFF=2;
    /** A shader pack redraws its whole pipeline per reflection, so only this many planes are live with one. */
    static final int MAX_SHADER_PLANES=2;
    /** Images of mirrors that stay out of view this long are freed. */
    static final long UNUSED_NANOS=5_000_000_000L;
    /** Frames between two looks at a plane that was out of sight. */
    static final int HIDDEN_RECHECK=4;
    /** Image sizes snap to this step so a mirror moving across the screen does not reallocate every frame. */
    static final int SIZE_STEP=32;
    static final int MIN_SIZE=16;

    public static long completedPasses;

    /**
     * Told about every frame. Normal play has none; a tool that measures the mirrors names its class in the
     * system property {@code simplymirrorsplus.observer}.
     */
    public interface Observer {
        void frameStart();
        /** @param colorTexture OpenGL name of the texture that holds the finished world frame */
        void frameEnd(MirrorEngine<?> engine,int colorTexture,int width,int height);
    }

    private static final Observer OBSERVER=observer();

    private static Observer observer() {
        String name=System.getProperty("simplymirrorsplus.observer");
        if(name==null)return null;
        try {
            return (Observer)Class.forName(name).getDeclaredConstructor().newInstance();
        } catch(ReflectiveOperationException|RuntimeException unusable) {
            Logger.getLogger("simplymirrorsplus").log(Level.WARNING,"Cannot use mirror observer "+name,unusable);
            return null;
        }
    }

    /** What a Minecraft version provides. {@code T} is that version's render target type. */
    public interface Platform<T> {
        /** A stable name for the mirror this face belongs to; faces of one connected mirror share it. */
        default String mirrorId(MirrorSurface face) { return ""; }
        /** A name for the world or server, under which per-mirror settings are stored. */
        default String worldKey() { return ""; }
        /** A camera object for a viewer that is not the player, as this platform's capture expects in {@link Pass#camera}. */
        default Object viewer(float yaw,float pitch) { return null; }
        T createTarget(int width,int height);
        void destroyTarget(T target);
        int width(T target);
        int height(T target);
        /** Mirror faces within {@code chunks} chunks of the eye; exposure and mode are checked separately every frame. */
        List<MirrorSurface> findSurfaces(double x,double y,double z,int chunks);
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
        /** True for a block of the world that nothing can be seen through; mirrors wholly behind such blocks are not reflected. */
        default boolean solid(int x,int y,int z) { return false; }
        /** Draws what sits on mirrors without being a reflection, their frames, into {@code into} (null means the main target). */
        default void decorate(T into,float[] viewProjection,double[] eye) { }
        /** OpenGL name of the colour texture behind {@link Pass#target}; an {@link Observer} reads the finished frame from it. */
        default int colorTexture(Object target) { return 0; }
    }

    /** One rendered view: the player's camera or a reflection of it. */
    public static final class Pass {
        /** Version-specific camera object, set by the platform when it creates the reflected camera. */
        public Object camera;
        /** Where this view is drawn: the main target for the player's camera, the capture image for a reflection. */
        public Object target;
        public final double[] eye;
        public final float[] view,projection;
        /**
         * The projection to cull this view with. For a reflection it is the image's projection without the mirror's
         * clip plane, which would skew the far side. A platform that sets a frustum for a reflection must set the
         * parent's back from this afterwards.
         */
        public float[] cull;
        public final int depth;
        public Pass(Object camera,double[] eye,float[] view,float[] projection,int depth) {
            this.camera=camera;this.eye=eye;this.view=view;this.projection=projection;this.depth=depth;
            cull=projection;
        }
        public float[] viewProjection() { return MirrorMatrices.multiply(projection,view); }
    }

    public static final class Capture<T> {
        public final Pass parent,self;
        public final MirrorSurface surface;
        public final T into;
        public final boolean playersOnly,shaders;
        /** The same as {@code self.cull}: what to cull this reflection with. */
        public final float[] perspective;
        public final MirrorTemporalHistory.Pose prior;
        /** Mirrors seen in this reflection; null when they are not rendered (shaders, Players Only). */
        public final Runnable nested;
        /**
         * Each mirror on this plane as a whole, as left, bottom, right, top along {@link MirrorSurface#across};
         * backdrops are laid out on these. Null unless the reflection is Players Only.
         */
        public final List<double[]> panels;
        /** What a Players Only reflection of this mirror shows behind the players. */
        public final MirrorBackdrop backdrop;
        Capture(Pass parent,Pass self,MirrorSurface surface,T into,boolean playersOnly,boolean shaders,float[] perspective,MirrorTemporalHistory.Pose prior,Runnable nested,List<double[]> panels,MirrorBackdrop backdrop) {
            this.parent=parent;this.self=self;this.surface=surface;this.into=into;this.playersOnly=playersOnly;
            this.shaders=shaders;this.perspective=perspective;this.prior=prior;this.nested=nested;this.panels=panels;this.backdrop=backdrop;
        }
        /** Mirror centre relative to the reflected eye, as ShaderReflection expects it. */
        public double offsetX() { return surface.px-self.eye[0]; }
        public double offsetY() { return surface.py-self.eye[1]; }
        public double offsetZ() { return surface.pz-self.eye[2]; }
    }

    /** Screen area a reflection needs, in normalized device coordinates, and the image size for it. */
    static final class Crop {
        final double x0,y0,x1,y1;
        final int width,height;
        /** The mirror's own bounds inside this area, in the area's clip space. */
        final float[] frame;
        Crop(double x0,double y0,double x1,double y1,int width,int height,float[] frame) {
            this.x0=x0;this.y0=y0;this.x1=x1;this.y1=y1;this.width=width;this.height=height;this.frame=frame;
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
        /** The frame this plane was last drawn in; -1 before the first time. */
        int renderedFrame=-1;
        T target;
        List<MirrorSurface> faces;
        String id="";
        int mode;
        int seenFrame=-2;
        long lastSeen;
        float[] capturedViewProjection;
        double[] capturedEye;
    }

    private static final class Group {
        final String key,id;
        final int mode;
        final List<MirrorSurface> faces=new ArrayList<MirrorSurface>();
        Group(String key,String id,int mode) { this.key=key;this.id=id;this.mode=mode; }
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
    /** Images of mirrors seen inside reflections; each bounce of a frame takes the next one. */
    private final List<T> nested=new ArrayList<T>();
    private int nestedUsed;
    /** Planes found out of sight: the frame they were looked at, by plane key. */
    private final Map<String,int[]> sight=new java.util.HashMap<String,int[]>();
    private final com.malidev.simplymirrorsplus.math.MirrorSight.Solid solid=new com.malidev.simplymirrorsplus.math.MirrorSight.Solid() {
        @Override public boolean at(int x,int y,int z) { return platform.solid(x,y,z); }
    };
    /** When a frame last needed no bounce images at all; they are freed some time after. */
    private long nestedNeeded;
    private final double[] corners=new double[12];
    private List<MirrorSurface> cached=new ArrayList<MirrorSurface>();
    /** How far each mirror reaches on each of its planes, by plane key and mirror; worked out when asked for and dropped with {@link #cached}. */
    private final Map<String,double[]> extents=new HashMap<String,double[]>();
    private final List<Slot<T>> planes=new ArrayList<Slot<T>>();
    private Pass root;
    private int mainWidth,mainHeight;
    private Object lastLevel;
    private MirrorConfig.Performance searchedTier;
    private int frame,drawnPlanes;
    private String previewId="";
    private int previewWidth,previewHeight;
    private T previewTarget;
    private float[] previewFrame={-1,-1,1,1};
    private long previewDue;
    private boolean failed;

    public MirrorEngine(Platform<T> platform) {
        this.platform=platform;
    }

    /**
     * Draws the reflections that are due into their images. Call it before the player's own view is drawn where
     * possible: other mods read the camera from the last world render of a frame, and that must be the player's.
     * @param root the player's camera, with the projection used to draw the frame
     * @param level any object that changes identity when the world changes
     */
    public void capture(Pass root,int mainWidth,int mainHeight,Object level,long now) {
        if(OBSERVER!=null)OBSERVER.frameStart();
        drawnPlanes=0;
        planes.clear();
        this.root=root;
        this.mainWidth=mainWidth;
        this.mainHeight=mainHeight;
        if(!MirrorConfig.enabled) { releaseTargets();return; }
        if(level!=lastLevel) { release();lastLevel=level;MirrorConfig.openWorld(platform.worldKey()); }
        int thisFrame=frame++;
        final double[] eye=root.eye;
        MirrorConfig.Performance tier=MirrorConfig.performance;
        if(thisFrame%20==0||tier!=searchedTier) { cached=platform.findSurfaces(eye[0],eye[1],eye[2],tier.chunks);searchedTier=tier;extents.clear(); }
        MirrorTemporalHistory.Frame prior=temporal.advance(root.view,root.projection,eye[0],eye[1],eye[2]);
        if(nestedUsed>0)nestedNeeded=now;
        else if(!nested.isEmpty()&&now-nestedNeeded>UNUSED_NANOS)releaseNested();
        releaseUnused(now);
        nestedUsed=0;
        if(cached.isEmpty()||failed||!usable(root.projection))return;
        cached.sort(new Comparator<MirrorSurface>() {
            @Override public int compare(MirrorSurface a,MirrorSurface b) { return Double.compare(distance(a,eye),distance(b,eye)); }
        });
        boolean shaders=platform.shadersActive();
        Frame context=new Frame(root.projection,prior,shaders);
        float[] vp=root.viewProjection();
        for(Group group:visiblePlanes(root,null)) {
            if(planes.size()==tier.cachedPlanes)break;
            if(!inSight(group,eye,thisFrame))continue;
            Slot<T> slot=slots.get(group.key);
            if(slot==null) { slot=new Slot<T>();slots.put(group.key,slot); }
            slot.faces=group.faces;
            slot.id=group.id;
            slot.mode=group.mode;
            slot.lastSeen=now;
            planes.add(slot);
        }
        evictExtraSlots(tier.cachedPlanes);
        // The nearest planes are drawn every frame. The others take turns, the longest waiting first, and show
        // their last image meanwhile, so a distant mirror never falls back to its block texture.
        int live=Math.min(planes.size(),shaders?Math.min(MAX_SHADER_PLANES,tier.livePlanes):tier.livePlanes);
        List<Slot<T>> due=new ArrayList<Slot<T>>(planes.subList(0,live));
        List<Slot<T>> waiting=new ArrayList<Slot<T>>(planes.subList(live,planes.size()));
        waiting.sort(new Comparator<Slot<T>>() {
            @Override public int compare(Slot<T> a,Slot<T> b) { return Integer.compare(a.renderedFrame,b.renderedFrame); }
        });
        due.addAll(waiting.subList(0,Math.min(waiting.size(),tier.backgroundUpdates)));
        try {
            capturePreview(context,root,now);
            for(int i=0;i<due.size();i++) {
                Slot<T> slot=due.get(i);
                // Iris sizes its gbuffers from the depth target, so with shaders the image keeps the screen size.
                int percent=shaders?100:tier.resolution;
                int baseWidth=Math.max(MIN_SIZE,mainWidth*percent/100),baseHeight=Math.max(MIN_SIZE,mainHeight*percent/100);
                // An image that is not redrawn every frame needs a margin for the camera to move in.
                Crop crop=crop(slot.faces,vp,eye,baseWidth,baseHeight,shaders,i>=live);
                slot.target=fit(slot.target,crop,shaders);
                capture(context,root,slot.faces,slot.target,crop,slot.mode==MODE_PLAYERS,slot.id);
                slot.capturedViewProjection=flipped(crop.apply(vp));
                slot.capturedEye=eye.clone();
                slot.renderedFrame=thisFrame;
                completedPasses++;
            }
        } catch(RuntimeException exception) {
            fail(exception);
        }
    }

    /**
     * Asks for a picture of one mirror as seen from straight in front of it, for the settings screen.
     * An empty id stops it.
     */
    public void requestPreview(String id,int width,int height) {
        previewId=id==null||width<8||height<8?"":id;
        previewWidth=width;previewHeight=height;
        if(previewId.isEmpty()&&previewTarget!=null) { platform.destroyTarget(previewTarget);previewTarget=null; }
    }

    /** The preview image, or null before the first one is taken. */
    public T previewTarget() { return previewTarget; }
    /** The mirror's bounds inside the preview image, in clip space. */
    public float[] previewFrame() { return previewFrame; }

    /**
     * Runs before the player's own mirrors so that the last capture of the frame, whose camera the platform
     * leaves in place, belongs to the player.
     * ponytail: skipped with a shader pack, whose reflections need the screen's exact size.
     */
    private void capturePreview(Frame context,Pass root,long now) {
        if(previewId.isEmpty()||context.shaders||now<previewDue)return;
        previewDue=now+50_000_000L;
        // Of the mirror's planes, show the one with the most faces that look towards the player.
        Map<String,List<MirrorSurface>> planes=new LinkedHashMap<String,List<MirrorSurface>>();
        List<MirrorSurface> faces=null;
        int mode=MODE_NONE;
        for(MirrorSurface face:cached) {
            if(!platform.mirrorId(face).equals(previewId))continue;
            int faceMode=platform.faceMode(face);
            if(faceMode!=MODE_NORMAL&&faceMode!=MODE_PLAYERS||!face.frontFacing(root.eye[0],root.eye[1],root.eye[2],0))continue;
            List<MirrorSurface> plane=planes.get(face.planeKey());
            if(plane==null) { plane=new ArrayList<MirrorSurface>();planes.put(face.planeKey(),plane); }
            plane.add(face);
            if(faces==null||plane.size()>faces.size()) { faces=plane;mode=faceMode; }
        }
        if(faces==null)return;
        MirrorSurface first=faces.get(0);
        double cx=0,cy=0,cz=0,reach=.5;
        for(MirrorSurface face:faces) { cx+=face.cx;cy+=face.cy;cz+=face.cz; }
        cx/=faces.size();cy/=faces.size();cz/=faces.size();
        for(MirrorSurface face:faces)reach=Math.max(reach,.5+Math.max(Math.abs(face.cx-cx),Math.max(Math.abs(face.cy-cy),Math.abs(face.cz-cz))));
        double away=Math.max(2,reach*2.2);
        double[] eye={cx+first.nx*away,cy+first.ny*away,cz+first.nz*away};
        // Look back along the normal. A floor or ceiling mirror has no upright, so north is up for it.
        double fx=-first.nx,fy=-first.ny,fz=-first.nz,ux=0,uy=first.ny!=0?0:1,uz=first.ny!=0?-1:0;
        double rx=fy*uz-fz*uy,ry=fz*ux-fx*uz,rz=fx*uy-fy*ux;
        float[] view={(float)rx,(float)ux,(float)-fx,0,(float)ry,(float)uy,(float)-fy,0,(float)rz,(float)uz,(float)-fz,0,0,0,0,1};
        // The player's own projection keeps this platform's depth convention; only its width is fitted to the panel.
        float[] projection=root.projection.clone();
        projection[0]=Math.copySign(Math.abs(projection[5])*previewHeight/previewWidth,projection[0]);
        projection[8]=projection[9]=0;
        float yaw=(float)Math.toDegrees(Math.atan2(-fx,fz)),pitch=(float)Math.toDegrees(-Math.asin(fy));
        Pass viewer=new Pass(platform.viewer(yaw,pitch),eye,view,projection,0);
        viewer.target=root.target;
        if(MirrorConfig.mode(previewId)==MODE_PLAYERS&&mode==MODE_NORMAL)mode=MODE_PLAYERS;
        Crop crop=crop(faces,viewer.viewProjection(),eye,Math.max(MIN_SIZE,previewWidth),Math.max(MIN_SIZE,previewHeight),false,false);
        if(previewTarget==null||platform.width(previewTarget)!=crop.width||platform.height(previewTarget)!=crop.height) {
            if(previewTarget!=null)platform.destroyTarget(previewTarget);
            previewTarget=platform.createTarget(crop.width,crop.height);
        }
        capture(new Frame(projection,context.prior,false),viewer,faces,previewTarget,crop,mode==MODE_PLAYERS,previewId);
        previewFrame=flippedFrame(crop.frame);
    }

    /**
     * False for a projection nothing can be drawn with. The game hands one over for the first frames after a
     * world is joined, when its field of view still opens from zero.
     */
    static boolean usable(float[] projection) {
        for(float value:projection)if(Float.isNaN(value)||Float.isInfinite(value))return false;
        // The scale of a view narrower than about a hundredth of a degree.
        return projection[0]!=0&&projection[5]!=0&&Math.abs(projection[5])<1e4f;
    }

    /** Draws the captured images onto the mirrors in the player's view, once that view exists. */
    public void composite() {
        if(root==null)return;
        try {
            float[] vp=root.viewProjection();
            for(Slot<T> slot:planes) {
                if(slot.target==null||failed)continue;
                platform.composite(null,slot.target,vp,root.eye,slot.capturedViewProjection,slot.capturedEye,slot.faces);
                drawnPlanes++;
            }
            if(!failed)platform.decorate(null,vp,root.eye);
        } catch(RuntimeException exception) {
            fail(exception);
        } finally {
            if(OBSERVER!=null)OBSERVER.frameEnd(this,platform.colorTexture(root.target),mainWidth,mainHeight);
            root=null;
            planes.clear();
        }
    }

    private void fail(RuntimeException exception) {
        failed=true;
        LOGGER.log(Level.SEVERE,"Mirror reflection disabled for this world after a rendering error",exception);
    }

    private void capture(Frame frame,Pass parent,List<MirrorSurface> faces,T into,Crop crop,boolean playersOnly,String id) {
        MirrorSurface surface=faces.get(0);
        double[] eye=surface.plane().reflectPoint(parent.eye[0],parent.eye[1],parent.eye[2]);
        float[] view=MirrorMatrices.multiply(parent.view,ReflectionTransform.matrix(surface.nx,surface.ny,surface.nz));
        // Across the image the reflection continues its parent's projection, which may itself be a part of the
        // screen; in depth it gets a clip plane on the mirror. The two do not touch each other's rows.
        float[] projection=parent.cull.clone();
        if(!frame.shaders) {
            float[] clipped=MirrorMatrices.oblique(frame.perspective,view,surface.nx,surface.ny,surface.nz,
                    surface.px-eye[0],surface.py-eye[1],surface.pz-eye[2],platform.depthZeroToOne(),platform.depthReversed());
            for(int column=0;column<4;column++)projection[column*4+2]=clipped[column*4+2];
        }
        final Pass self=new Pass(null,eye,view,flipped(crop.apply(projection)),parent.depth+1);
        self.target=into;
        self.cull=flipped(crop.apply(parent.cull));
        final MirrorSurface parentSurface=surface;
        final T target=into;
        final Frame context=frame;
        // ponytail: Iris reflection state is single-level, so shader packs stop at one bounce.
        Runnable nestedMirrors=frame.shaders||playersOnly?null:new Runnable() {
            @Override public void run() { renderMirrorsInReflection(context,self,parentSurface,target); }
        };
        platform.capture(new Capture<T>(parent,self,surface,into,playersOnly,frame.shaders,self.cull,frame.prior.reflect(surface),nestedMirrors,playersOnly?panels(faces):null,MirrorConfig.backdrop(id)));
    }

    /**
     * The mirrors these faces belong to, each as the rectangle it takes up on the faces' plane. The rectangle
     * holds the mirror's faces that are out of view too, so it stays put while the player moves and turns.
     */
    private List<double[]> panels(List<MirrorSurface> faces) {
        List<double[]> panels=new ArrayList<double[]>();
        for(MirrorSurface face:faces) {
            String id=platform.mirrorId(face),key=face.planeKey()+"/"+id;
            double[] extent=extents.get(key);
            if(extent==null) {
                extent=new double[]{Double.MAX_VALUE,Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
                for(MirrorSurface other:cached)if(other.planeKey().equals(face.planeKey())&&platform.mirrorId(other).equals(id))other.extend(extent);
                // A face that was found after the last search is not in the list yet.
                face.extend(extent);
                extents.put(key,extent);
            }
            if(!panels.contains(extent))panels.add(extent);
        }
        return panels;
    }

    /**
     * A reflection turns the world's handedness around. Drawn as it is, every triangle would face the other
     * way, and anything that draws by its own rules, as some mods' models do, would come out inside out. So the
     * image is drawn flipped left to right, which turns the handedness back, and is flipped again when it is put
     * on the mirror: both this and the lookup go through the same matrix.
     */
    static float[] flipped(float[] matrix) {
        float[] out=matrix.clone();
        for(int column=0;column<4;column++)out[column*4]=-out[column*4];
        return out;
    }

    /** The mirror's bounds inside a flipped image. Left and right keep their order, so the preview that is cut out along them is flipped back. */
    private static float[] flippedFrame(float[] frame) { return new float[]{-frame[0],frame[1],-frame[2],frame[3]}; }

    /**
     * Without this a mirror seen inside a reflection shows its block texture. The nearest planes get a real
     * bounce while the frame's budget lasts; the rest reuse images that already exist, which costs no world render.
     */
    private void renderMirrorsInReflection(Frame frame,Pass self,MirrorSurface parentSurface,T into) {
        float[] vp=self.viewProjection();
        MirrorConfig.Performance tier=MirrorConfig.performance;
        for(Group group:visiblePlanes(self,parentSurface)) {
            T source;
            float[] sourceViewProjection;
            double[] sourceEye=self.eye;
            if(self.depth<tier.bounces&&nestedUsed<tier.nestedBudget) {
                int index=nestedUsed++;
                while(nested.size()<=index)nested.add(null);
                Crop crop=crop(group.faces,vp,self.eye,platform.width(into),platform.height(into),false,false);
                T image=fit(nested.get(index),crop,false);
                nested.set(index,image);
                capture(frame,self,group.faces,image,crop,group.mode==MODE_PLAYERS,group.id);
                source=image;
                sourceViewProjection=flipped(crop.apply(vp));
            } else {
                Slot<T> slot=slots.get(group.key);
                if(slot==null||slot.target==null||slot.target==into||slot.capturedEye==null)continue;
                // ponytail: this image was taken from the main camera, so its perspective is approximate at this depth.
                source=slot.target;
                sourceViewProjection=slot.capturedViewProjection;
                sourceEye=slot.capturedEye;
            }
            platform.composite(into,source,vp,self.eye,sourceViewProjection,sourceEye,group.faces);
        }
        platform.decorate(into,vp,self.eye);
    }

    /** Visible planes nearest first, split by mode; {@code behind} limits them to what lies in front of that mirror. */
    private List<Group> visiblePlanes(Pass view,final MirrorSurface behind) {
        final Map<MirrorSurface,Integer> modes=new IdentityHashMap<MirrorSurface,Integer>();
        final Map<MirrorSurface,String> ids=new IdentityHashMap<MirrorSurface,String>();
        List<MirrorSurface> selected=MirrorFaceSelector.select(cached,view.viewProjection(),view.eye[0],view.eye[1],view.eye[2],
                MirrorFaceSelector.MAX_VISIBLE_FACES,MirrorConfig.performance.chunks*16,new java.util.function.Predicate<MirrorSurface>() {
                    @Override public boolean test(MirrorSurface face) {
                        // Geometry behind the parent mirror was clipped away, so nothing there may be composited.
                        if(behind!=null&&!behind.frontFacing(face.cx,face.cy,face.cz,.01))return false;
                        int mode=platform.faceMode(face);
                        String id=platform.mirrorId(face);
                        ids.put(face,id);
                        // A mirror's own setting can narrow what its blocks ask for, never widen it.
                        int wanted=MirrorConfig.mode(id);
                        if(mode==MODE_NORMAL&&wanted!=MODE_NORMAL||mode==MODE_PLAYERS&&wanted==MODE_OFF)mode=wanted;
                        modes.put(face,mode);
                        return mode==MODE_NORMAL||mode==MODE_PLAYERS;
                    }
                });
        Map<String,Group> groups=new LinkedHashMap<String,Group>();
        for(MirrorSurface face:selected) {
            int mode=modes.get(face);
            // Mirrors on one plane share an image unless one has settings of its own, which may differ.
            String id=MirrorConfig.mirror(ids.get(face))==null?"":ids.get(face),key=face.planeKey()+"/"+mode+"/"+id;
            Group group=groups.get(key);
            if(group==null) { group=new Group(key,id,mode);groups.put(key,group); }
            group.faces.add(face);
        }
        return new ArrayList<Group>(groups.values());
    }

    /**
     * Smallest screen area holding the faces.
     * @param margin pads the area so an image that is not redrawn every frame still covers the mirror while the camera moves
     */
    Crop crop(List<MirrorSurface> faces,float[] viewProjection,double[] eye,int baseWidth,int baseHeight,boolean fullScreen,boolean margin) {
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
        double fx0=x0,fy0=y0,fx1=x1,fy1=y1;
        boolean framed=!whole;
        if(!whole) {
            // Two pixels keep the image's filtered border off the mirror's edge.
            double padX=margin?.05+(x1-x0)*.1:4d/baseWidth,padY=margin?.05+(y1-y0)*.1:4d/baseHeight;
            x0=Math.max(-1,x0-padX);x1=Math.min(1,x1+padX);y0=Math.max(-1,y0-padY);y1=Math.min(1,y1+padY);
            if(x1-x0<=0||y1-y0<=0)whole=true;
        }
        if(whole) { x0=-1;y0=-1;x1=1;y1=1; }
        int width=fullScreen?baseWidth:snap(baseWidth*(x1-x0)/2,baseWidth);
        int height=fullScreen?baseHeight:snap(baseHeight*(y1-y0)/2,baseHeight);
        float[] frame=framed?new float[]{(float)((fx0-x0)/(x1-x0)*2-1),(float)((fy0-y0)/(y1-y0)*2-1),(float)((fx1-x0)/(x1-x0)*2-1),(float)((fy1-y0)/(y1-y0)*2-1)}:new float[]{-1,-1,1,1};
        return new Crop(x0,y0,x1,y1,width,height,frame);
    }

    private static int snap(double size,int limit) {
        int stepped=(int)Math.ceil(Math.max(MIN_SIZE,size)/SIZE_STEP)*SIZE_STEP;
        return Math.max(MIN_SIZE,Math.min(limit,stepped));
    }

    /**
     * Whether any of the plane's faces can be seen from the eye. A plane found hidden is looked at again only
     * every few frames, since walking the sight lines of a hidden mirror is the costly case.
     */
    private boolean inSight(Group group,double[] eye,int thisFrame) {
        int[] known=sight.get(group.key);
        if(known!=null&&known[1]==0&&thisFrame-known[0]<HIDDEN_RECHECK)return false;
        boolean seen=com.malidev.simplymirrorsplus.math.MirrorSight.any(group.faces,eye[0],eye[1],eye[2],solid);
        if(seen)sight.remove(group.key);
        else { if(sight.size()>256)sight.clear();sight.put(group.key,new int[]{thisFrame,0}); }
        return seen;
    }

    /**
     * The image to draw into: the old one while it is large enough and not wastefully large, otherwise a new
     * one with room to grow, so a mirror the player walks up to does not get a new image every few steps.
     */
    private T fit(T image,Crop crop,boolean exact) {
        if(image!=null) {
            int width=platform.width(image),height=platform.height(image);
            if(exact?width==crop.width&&height==crop.height:width>=crop.width&&height>=crop.height&&width<=crop.width*2&&height<=crop.height*2)return image;
            platform.destroyTarget(image);
        }
        return exact?platform.createTarget(crop.width,crop.height):platform.createTarget(grown(crop.width),grown(crop.height));
    }

    private static int grown(int size) { return (size+size/4+SIZE_STEP-1)/SIZE_STEP*SIZE_STEP; }

    private void releaseUnused(long now) {
        for(Iterator<Slot<T>> it=slots.values().iterator();it.hasNext();) {
            Slot<T> slot=it.next();
            if(now-slot.lastSeen>UNUSED_NANOS) { if(slot.target!=null)platform.destroyTarget(slot.target);it.remove(); }
        }
    }

    private void evictExtraSlots(int limit) {
        while(slots.size()>limit) {
            Map.Entry<String,Slot<T>> oldest=null;
            for(Map.Entry<String,Slot<T>> entry:slots.entrySet())
                if(oldest==null||entry.getValue().lastSeen<oldest.getValue().lastSeen)oldest=entry;
            if(oldest.getValue().target!=null)platform.destroyTarget(oldest.getValue().target);
            slots.remove(oldest.getKey());
        }
    }

    /** Every mirror surface the last search found. */
    public List<MirrorSurface> surfaces() { return cached; }

    /** Mirror planes drawn into the player's view by the last frame. */
    int drawnPlanes() { return drawnPlanes; }

    /** True once a rendering error has switched reflections off for this world. */
    boolean failed() { return failed; }

    /** Number of render targets currently held, for tests and memory checks. */
    int heldTargets() {
        int count=0;
        for(Slot<T> slot:slots.values())if(slot.target!=null)count++;
        for(T image:nested)if(image!=null)count++;
        return count;
    }

    public void releaseTargets() {
        if(previewTarget!=null) { platform.destroyTarget(previewTarget);previewTarget=null; }
        for(Slot<T> slot:slots.values())if(slot.target!=null)platform.destroyTarget(slot.target);
        slots.clear();
        releaseNested();
    }

    private void releaseNested() {
        for(T image:nested)if(image!=null)platform.destroyTarget(image);
        nested.clear();
    }

    public void release() {
        releaseTargets();
        cached=new ArrayList<MirrorSurface>();
        extents.clear();
        lastLevel=null;
        frame=0;
        failed=false;
        sight.clear();
        temporal.reset();
    }

    private static double distance(MirrorSurface s,double[] eye) {
        double x=s.cx-eye[0],y=s.cy-eye[1],z=s.cz-eye[2];
        return x*x+y*y+z*z;
    }
}
