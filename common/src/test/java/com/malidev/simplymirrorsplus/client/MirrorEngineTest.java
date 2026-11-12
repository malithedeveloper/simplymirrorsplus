package com.malidev.simplymirrorsplus.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.malidev.simplymirrorsplus.math.MirrorSurface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class MirrorEngineTest {
    private static final long FRAME=1_000_000_000L/144;
    private static final double[] EYE={.5,.5,0};

    /** Fake target: {width, height}. */
    private static final class Fake implements MirrorEngine.Platform<int[]> {
        final List<MirrorSurface> world=new ArrayList<MirrorSurface>();
        final Map<MirrorSurface,Integer> modes=new HashMap<MirrorSurface,Integer>();
        final List<MirrorEngine.Capture<int[]>> captures=new ArrayList<MirrorEngine.Capture<int[]>>();
        int created,destroyed,composites;
        @Override public int[] createTarget(int width,int height) { created++;return new int[]{width,height}; }
        @Override public void destroyTarget(int[] target) { destroyed++; }
        @Override public int width(int[] target) { return target[0]; }
        @Override public int height(int[] target) { return target[1]; }
        @Override public List<MirrorSurface> findSurfaces(double x,double y,double z) { return new ArrayList<MirrorSurface>(world); }
        @Override public int faceMode(MirrorSurface face) { Integer mode=modes.get(face);return mode==null?MirrorEngine.MODE_NORMAL:mode; }
        @Override public boolean shadersActive() { return false; }
        @Override public boolean depthZeroToOne() { return false; }
        @Override public void capture(MirrorEngine.Capture<int[]> capture) { captures.add(capture);if(capture.nested!=null)capture.nested.run(); }
        @Override public void composite(int[] into,int[] source,float[] vp,double[] eye,float[] sourceVp,double[] sourceEye,List<MirrorSurface> faces) { composites++; }
    }

    private Fake fake;
    private MirrorEngine<int[]> engine;
    private final Object level=new Object();

    /** OpenGL-style perspective looking down -z, column-major like the game's matrices. */
    private static float[] perspective() {
        float f=(float)(1/Math.tan(Math.toRadians(35))),near=.05f,far=256;
        float[] m=new float[16];
        m[0]=f/(16f/9);m[5]=f;m[10]=(far+near)/(near-far);m[11]=-1;m[14]=2*far*near/(near-far);
        return m;
    }

    private static float[] identity() { float[] m=new float[16];m[0]=m[5]=m[10]=m[15]=1;return m; }

    private void frames(int count,long start) {
        for(int i=0;i<count;i++)engine.render(new MirrorEngine.Pass(null,EYE,identity(),perspective(),0),1920,1080,level,start+i*FRAME);
    }

    @Before public void setUp() {
        MirrorConfig.enabled=true;MirrorConfig.playersOnly=false;MirrorConfig.refreshRate=60;MirrorConfig.resolutionPercent=100;
        fake=new Fake();
        engine=new MirrorEngine<int[]>(fake);
        // A 1x1 mirror 3 blocks ahead, facing the camera.
        fake.world.add(new MirrorSurface(0,0,-4,0,0,1));
    }

    @Test public void cropCoversOnlyTheMirror() {
        MirrorEngine.Crop crop=engine.crop(fake.world,perspective(),EYE,1920,1080,false);
        assertTrue(crop.x0>-1&&crop.x1<1&&crop.width<1920&&crop.height<1080);
        assertEquals(0,crop.width%MirrorEngine.SIZE_STEP);
        float[] corner=crop.apply(identity());
        double x=corner[0]*crop.x0+corner[12],y=corner[5]*crop.y0+corner[13];
        assertEquals(-1,x,1e-5);
        assertEquals(-1,y,1e-5);
        MirrorEngine.Crop full=engine.crop(fake.world,perspective(),EYE,1920,1080,true);
        assertEquals(1920,full.width);
        assertEquals(1080,full.height);
    }

    @Test public void cornerBehindCameraFallsBackToTheWholeScreen() {
        List<MirrorSurface> behind=Arrays.asList(new MirrorSurface(0,0,0,0,0,1));
        MirrorEngine.Crop crop=engine.crop(behind,perspective(),EYE,1920,1080,false);
        assertEquals(-1,crop.x0,0);
        assertEquals(1,crop.y1,0);
    }

    @Test public void holdsTheRefreshRateAndReusesImages() {
        frames(144,0);
        assertEquals(60,fake.captures.size(),1);
        assertEquals(144,fake.composites);
        // One image for the mirror plus at most one spare being swapped with it.
        assertTrue("created "+fake.created,fake.created<=2);
        assertTrue(engine.heldTargets()<=2);
    }

    @Test public void freesImagesOfMirrorsOutOfView() {
        frames(10,0);
        assertTrue(engine.heldTargets()>0);
        fake.world.clear();
        fake.modes.clear();
        frames(21,10*FRAME);
        frames(1,10*FRAME+MirrorEngine.UNUSED_NANOS+FRAME*30);
        assertEquals(0,engine.heldTargets());
        assertEquals(fake.created,fake.destroyed);
    }

    @Test public void modesDecideWhatIsRendered() {
        MirrorSurface face=fake.world.get(0);
        fake.modes.put(face,MirrorEngine.MODE_OFF);
        frames(30,0);
        assertTrue(fake.captures.isEmpty());
        assertEquals(0,fake.composites);
        fake.modes.put(face,MirrorEngine.MODE_PLAYERS);
        frames(1,30*FRAME);
        assertTrue(fake.captures.get(0).playersOnly);
        assertNull(fake.captures.get(0).nested);
        fake.captures.clear();
        fake.modes.put(face,MirrorEngine.MODE_NORMAL);
        MirrorConfig.playersOnly=true;
        frames(10,31*FRAME);
        assertFalse(fake.captures.isEmpty());
        assertTrue("global setting overrides normal mirrors",fake.captures.get(0).playersOnly);
    }

    @Test public void facingMirrorsGetOneRealBounce() {
        // Second mirror behind the camera, facing the first one.
        fake.world.add(new MirrorSurface(0,0,3,0,0,-1));
        frames(1,0);
        assertEquals(2,fake.captures.size());
        assertEquals(1,fake.captures.get(0).self.depth);
        assertEquals(2,fake.captures.get(1).self.depth);
        assertFalse(fake.captures.get(1).playersOnly);
        assertEquals("plane image and the bounce image",2,engine.heldTargets());
    }

    @Test public void disablingFreesEverything() {
        frames(5,0);
        MirrorConfig.enabled=false;
        frames(1,5*FRAME);
        assertEquals(0,engine.heldTargets());
        MirrorConfig.enabled=true;
    }
}
