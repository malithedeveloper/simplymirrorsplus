package com.malidev.simplymirrorsplus.math;

/** Holds a fixed refresh rate without drifting when the game frame rate is not a multiple of it. */
public final class MirrorRefreshClock {
    private long due;
    private boolean started;

    /** Sort key: smaller means longer overdue; a clock that never rendered sorts first. */
    public long due() { return started?due:Long.MIN_VALUE; }
    public boolean isDue(long now) { return !started||now-due>=0; }
    public void reset() { started=false; }
    public void rendered(long now,long interval) {
        // Advancing from the deadline keeps the average exact. After a stall, resynchronize instead of bursting.
        due=started&&now-due<interval?due+interval:now+interval;
        started=true;
    }
}
