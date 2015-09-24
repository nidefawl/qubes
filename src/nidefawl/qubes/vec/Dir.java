package nidefawl.qubes.vec;

public class Dir {
    public final static int DIR_POS_X = 0;
    public final static int DIR_NEG_X = 1;
    public final static int DIR_POS_Y = 2;
    public final static int DIR_NEG_Y = 3;
    public final static int DIR_POS_Z = 4;
    public final static int DIR_NEG_Z = 5;

    public static final int getDirX(int s) {
        return s == 0 ? -1 : s == 1 ? 1 : 0;
    }

    public static final int getDirZ(int s) {
        return s == 2 ? -1 : s == 3 ? 1 : 0;
    }

    public static final int getDirY(int s) {
        return s == 4 ? -1 : s == 5 ? 1 : 0;
    }

    /**
     * @param faceDir
     * @return
     */
    public static boolean isTopBottom(int faceDir) {
        return faceDir == DIR_POS_Y || faceDir == DIR_NEG_Y;
    }
}
