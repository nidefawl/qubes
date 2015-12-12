/**
 * 
 */
package nidefawl.qubes.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ServerStats {
    static ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>();
    public static void add(String string, long took) {
        Long l = map.get(string);
        if (l == null)
            l = 0L;
        l+=took;
        map.put(string, l);
    }
    /**
     * 
     */
    public static void dump() {
        for (String s : map.keySet()) {
            System.out.printf("%-32s %20s\n", s, ""+map.get(s)/1000000L);
        }
        Long l1 = map.get("generateChunk");
        Long l2 = map.get("generatedChunks");
        if (l1 != null && l2 != null) {
            double chunkTime = l1 / 1000000.0D;
            double l = chunkTime/(double)l2;
            System.out.printf("Per chunk: %.2fms\n", l);
        }
    }

}
