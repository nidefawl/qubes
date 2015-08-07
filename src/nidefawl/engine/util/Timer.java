/*
 //MODIFIED org/apache/jackrabbit/oak/stats/Clock.java
 */
package nidefawl.engine.util;


public class Timer {
    private final int tickspersec;

    private static final long NS_IN_MS = 1000000;
    private long ms = System.currentTimeMillis();
    private long ns = System.nanoTime();
    private long last = getTime();
    public int ticks;
    public float partialTick;

    public long el;
    public Timer(int tickspersec) {
        this.tickspersec=tickspersec;
    }
    
    public long getTime() {
        long nowns = System.nanoTime();
        long nsIncrease = Math.max(nowns - ns, 0); // >= 0

        long msIncrease = (nsIncrease + NS_IN_MS/2) / NS_IN_MS; // round up

        // If last clock sync was less than one second ago, the nanosecond
        // timer drift will be insignificant and there's no need to re-sync.
        if (msIncrease < 1000) {
            return ms + msIncrease;
        }

        // Last clock sync was up to ten seconds ago, so we synchronize
        // smoothly to avoid both drift and sudden jumps.
        long nowms = System.currentTimeMillis();
        if (msIncrease < 10000) {
            // 1) increase the ms and ns timestamps as if the estimated
            //    ms increase was entirely correct
            ms += msIncrease;
            ns += msIncrease * NS_IN_MS;
            // 2) compare the resulting time with the wall clock to see
            //    if we're out of sync and to adjust accordingly
            long jump = nowms - ms;
            if (jump == 0) {
                // 2a) No deviation from wall clock.
                return ms;
            } else if (0 < jump && jump < 100) {
                // 2b) The wall clock is up to 100ms ahead of us, probably
                // because of its low granularity. Adjust the ns timestamp
                // 0.1ms backward for future clock readings to jump that
                // much ahead to eventually catch up with the wall clock.
                ns -= NS_IN_MS / 10;
                return ms;
            } else if (0 > jump && jump > -100) {
                // 2c) The wall clock is up to 100ms behind us, probably
                // because of its low granularity. Adjust the ns timestamp
                // 0.1ms forward for future clock readings to stay constant
                // (because of the Math.max(..., 0) above) for that long
                // to eventually catch up with the wall clock.
                ns += NS_IN_MS / 10;
                return ms;
            }
        }

        // Last clock sync was over 10s ago or the nanosecond timer has
        // drifted more than 100ms from the wall clock, so it's best to
        // to a hard sync with no smoothing.
        if (nowms >= ms + 1000) {
            ms = nowms;
            ns = nowns;
        } else {
            // Prevent the clock from moving backwards by setting the
            // ms timestamp to exactly 1s ahead of the last sync time
            // (to account for the time between clock syncs), and
            // adjusting the ns timestamp ahead so that the reported time
            // will stall until the clock would again move ahead.
            ms = ms + 1000; // the 1s clock sync interval from above
            ns = nowns + (ms - nowms) * NS_IN_MS;
        }
        return ms;
    }

    public void calculate() {
        long n = getTime();
        this.el = n-last;
        last = n;
        this.partialTick += this.el/1000.0D*tickspersec;
        this.ticks = (int) this.partialTick;
        this.partialTick -= this.ticks;
    }


}