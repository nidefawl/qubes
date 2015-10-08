package nidefawl.qubes.vec;

public class Dir {
    public final static int DIR_POS_X = 0;
    public final static int DIR_NEG_X = 1;
    public final static int DIR_POS_Y = 2;
    public final static int DIR_NEG_Y = 3;
    public final static int DIR_POS_Z = 4;
    public final static int DIR_NEG_Z = 5;

    public static final int getDirX(int s) {
        return s == DIR_NEG_X ? -1 : s == DIR_POS_X ? 1 : 0;
    }

    public static final int getDirZ(int s) {
        return s == DIR_NEG_Z ? -1 : s == DIR_POS_Z ? 1 : 0;
    }

    public static final int getDirY(int s) {
        return s == DIR_NEG_Y ? -1 : s == DIR_POS_Y ? 1 : 0;
    }

    /**
     * @param faceDir
     * @return
     */
    public static boolean isTopBottom(int faceDir) {
        return faceDir == DIR_POS_Y || faceDir == DIR_NEG_Y;
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static int toInt(int x, int y, int z) {
        if (x < 0)
            return DIR_NEG_X;
        if (x > 0)
            return DIR_POS_X;
        if (y < 0)
            return DIR_NEG_Y;
        if (y > 0)
            return DIR_POS_Y;
        if (z < 0)
            return DIR_NEG_Z;
        if (z > 0)
            return DIR_POS_Z;
        return 0;
    }
    public static String toFaceName(int i) {
        switch (i) {
            case DIR_POS_X:
                return "X Positive";
            case DIR_NEG_X:
                return "X Negative";
            case DIR_POS_Y:
                return "Y Positive";
            case DIR_NEG_Y:
                return "Y Negative";
            case DIR_POS_Z:
                return "Z Positive";
            case DIR_NEG_Z:
                return "Z Negative";
        }
        return "No direction";
    }
}
