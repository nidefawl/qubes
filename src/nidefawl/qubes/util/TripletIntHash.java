package nidefawl.qubes.util;


public final class TripletIntHash {
    public static int toHash(int x, int y, int z) {
        x += 0x7FF;
        z += 0x7FF;
        return (x & 0xFFF) | ((z & 0xFFF) << 12) | ((y & 0xFF) << 24);
    }
    public static int getX(int hash) {
        return (hash & 0xFFF) - 0x7FF;
    }
    public static int getZ(int hash) {
        return ((hash >> 12) & 0xFFF) - 0x7FF;
    }
    public static int getY(int hash) {
        return (hash >> 24) & 0xFF;
    }
}
