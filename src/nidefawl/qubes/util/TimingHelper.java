package nidefawl.qubes.util;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;

public class TimingHelper {
    public static boolean useNanos = true;
    public static boolean init = false;
    public static int LEN = 1000;
    public static long nanos[] = new long[LEN];
    public static long millis[] = new long[LEN];
    public static long calls[] = new long[LEN];
    public static long beginNanos[] = new long[LEN];
    public static long beginMillis[] = new long[LEN];
    public static boolean on[] = new boolean[LEN];
    private static String[] names = new String[LEN];
    private static HashMap<Object, Integer> map = new HashMap<Object, Integer>();
    private static int jObjectIdx = 500;
    public static void startObj(Object o) {
        Integer idx = map.get(o);
        if (idx == null) {
            synchronized (map) {
                idx = map.get(o);
                if (idx == null) {
                    if (jObjectIdx >= LEN) return;
                    idx = jObjectIdx++;
                    map.put(o, idx);
                    names[idx] = o.toString();
                }
            }
        }
        start(idx);
    }
    public static void endObj(Object o) {
        Integer idx = map.get(o);
        if (idx != null) {
            end(idx);
        }
    }
    public static void start(int i) {
        if (!init) {
            long jvmuptime = ManagementFactory.getRuntimeMXBean().getUptime();
            if (jvmuptime < 2700) {
                return;
            }
            init = true;
        }
        beginMillis[i] = System.currentTimeMillis();
        if (useNanos)
            beginNanos[i] = System.nanoTime();
        if (on[i]) System.err.println("ALREADY ON: "+i);
        on[i] = true;
    }
    public static void startSilent(int i) {
        beginMillis[i] = System.currentTimeMillis();
        if (useNanos)
            beginNanos[i] = System.nanoTime();
    }
    public static long stopSilent(int i) {
        if (useNanos) {
            long nanosPassed = System.nanoTime() - beginNanos[i];
            if (nanosPassed < 1000000L) { 
                return nanosPassed/1000L;
            }
            return nanosPassed/1000L;
        }
        long millisPassed = System.currentTimeMillis() - beginMillis[i];
        return millisPassed*1000L;
    }
    public static long end(int i) {
        if (!init) return 0;
        if (!on[i]) System.err.println("NOT ON: "+i);
        on[i] = false;
        long millisPassed = System.currentTimeMillis() - beginMillis[i];
        long timeTaken = millisPassed*1000;
        if (millisPassed > 0) {
            millis[i] += millisPassed;
        } else if (useNanos) {
            long nanosPassed = System.nanoTime() - beginNanos[i];
            nanos[i] += nanosPassed;
            timeTaken += nanosPassed/1000;
            if (nanos[i] > 1000*1000) {
                millis[i]++;
                nanos[i] -= 1000*1000;
            }
        }
        
        if (calls[i]++%700220==0) {
            float perCall = (float) millis[i] / (float) calls[i];
            String n1=""+i;
            if (hasName(i)) {
                n1 = names[i];
            }
            n1 = String.format("%-20s",n1);
            System.out.println(String.format("%s %d calls. %.5f millis per call, %d ms total", n1, calls[i], perCall, millis[i]));
        }
        return timeTaken;
    }
    public static void dump() {
        for (int i = 0; i < calls.length; i++) {
            if (calls[i] > 0) {
                float perCall = (float) millis[i] / (float) calls[i];
                System.out.println(String.format("%2d %-20s %d calls. %.5f millis per call, %d ms total", i, hasName(i) ? names[i] : ""+i, calls[i], perCall, millis[i]));                
            }
        }
    }
    public static void setName(int i, String name) {
        names [i] = name;
    }
    public static boolean hasName(int i) {
        return names[i] != null;
    }
    public static void reset(int a, int i) {
        if (calls[a]>=i) {
            nanos[a] = 0;
            millis[a] = 0;
            calls[a] = 0;
        }
    }
    public static void reset() {
        Arrays.fill(calls, 0);
        Arrays.fill(nanos, 0);
        Arrays.fill(millis, 0);
        init = false;
    }
}
