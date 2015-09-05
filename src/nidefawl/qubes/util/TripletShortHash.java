package nidefawl.qubes.util;


public final class TripletShortHash {
    public static short toHash(int x, int y, int z) {
        return (short) (x << 12 | z << 8 | y);
    }
    public static int getX(int hash) {
        return hash >> 12 & 15;
    }
    public static int getZ(int hash) {
        return hash >> 8 & 15;
    }
    public static int getY(int hash) {
        return hash & 255;
    }
}
