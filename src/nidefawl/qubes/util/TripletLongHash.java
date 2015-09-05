package nidefawl.qubes.util;


public final class TripletLongHash {
    public static long toHash(long x, long y, long z) {
        x += 0x7FFFFF;
        z += 0x7FFFFF;
        return (x & 0xFFFFFF) | ((z & 0xFFFFFF) << 24) | ((y & 0xFFFF) << 48);
    }
    public static int getX(long hash) {
        return (int) ((hash & 0xFFFFFFL) - 0x7FFFFFL);
    }
    public static int getZ(long hash) {
        return (int) (((hash >> 24) & 0xFFFFFFL) - 0x7FFFFFL);
    }
    public static int getY(long hash) {
        return (int) ((hash >> 48) & 0xFFFFL);
    }
}
