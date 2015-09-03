package nidefawl.qubes.test;

import java.util.Random;

import nidefawl.qubes.util.GameMath;

public class CoordLongHash {

    public static int lhToZ(long l) {
        return (int) (l&0xFFFFFFFF) + Integer.MIN_VALUE;
    }
    public static int lhToX(long l) {
        return (int) (l >> 32);
    }
    public static long toLong(int x, int z) {
        return ((long) x << 32) |((long)z-Integer.MIN_VALUE);
    }
    public static void main(String[] args) {
        Random rand = new Random();
        int c=0;
        for (int a = 0; a < 72240000; a++) {
            int x = rand.nextInt();
            int z = rand.nextInt();
            long l = toLong(x, z);
            int x1 = lhToX(l);
            int z1 = lhToZ(l);
            if (x != x1 || z != z1) {
                System.err.println("FAILED: "+x+"/"+z+" != "+x1+"/"+z1);
                System.out.println("LONG: "+Long.toHexString(l));
                dump(l, "1");
                return;
            }
            c++;
        }
        System.out.println(c);
//        int x = -64000;
//        int z = 467;
//        long l = ((long) x << 32) |(long)(z-Integer.MIN_VALUE);
//      dump(l, "1");
//       l = ((long) x << 32) |(long)((long)z-Integer.MIN_VALUE);
//    dump(l, "2");
    }

    private static void dump(long l, String a) {
        String longBinStr = getBinary(l);
        System.out.println(a+ " "+lhToX(l)+"/"+lhToZ(l));
        System.out.println(a+ " MSB: "+longBinStr.substring(0, 32));
        System.out.println(a+ " LSB: "+longBinStr.substring(32));
        System.out.println();
    }
    private static String getBinary(long l) {
        String s = Long.toBinaryString(l);
        while (s.length() < 64) {
            s = "0"+s;
        }
        return s;
    }

}
