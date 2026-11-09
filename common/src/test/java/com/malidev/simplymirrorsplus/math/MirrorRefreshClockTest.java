package com.malidev.simplymirrorsplus.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MirrorRefreshClockTest {
    private static final long SECOND=1_000_000_000L;

    private static int updatesInOneSecond(int gameFps,int refreshRate) {
        MirrorRefreshClock clock=new MirrorRefreshClock();
        long interval=SECOND/refreshRate;
        int updates=0;
        for(int frame=0;frame<gameFps;frame++) {
            long now=frame*SECOND/gameFps;
            if(clock.isDue(now)) { clock.rendered(now,interval);updates++; }
        }
        return updates;
    }

    @Test public void holdsRateWhenFrameRateIsNotAMultiple() {
        assertEquals(60,updatesInOneSecond(144,60),1);
        assertEquals(90,updatesInOneSecond(240,90),1);
    }

    @Test public void updatesEveryFrameWhenGameIsSlowerThanRate() {
        assertEquals(40,updatesInOneSecond(40,120));
    }

    @Test public void doesNotBurstAfterStall() {
        MirrorRefreshClock clock=new MirrorRefreshClock();
        long interval=SECOND/60;
        clock.rendered(0,interval);
        long afterStall=SECOND;
        assertTrue(clock.isDue(afterStall));
        clock.rendered(afterStall,interval);
        assertFalse(clock.isDue(afterStall+1));
    }

    @Test public void resetForcesImmediateRefresh() {
        MirrorRefreshClock clock=new MirrorRefreshClock();
        clock.rendered(0,SECOND);
        assertFalse(clock.isDue(1));
        clock.reset();
        assertTrue(clock.isDue(1));
    }
}
