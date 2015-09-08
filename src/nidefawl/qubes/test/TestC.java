package nidefawl.qubes.test;

public class TestC {

    public static long toHash(long x, long y, long z, long i) {
        x += 0x7FFFFF;
        z += 0x7FFFFF;
        return (x & 0xFFFFFF) | ((z & 0xFFFFFF) << 24) | ((y & 0x1FF) << 48) | ((i& 0b111)<<(48+9));
    }

    public static int getX(long hash) {
        return (int) ((hash & 0xFFFFFFL) - 0x7FFFFFL);
    }

    public static int getZ(long hash) {
        return (int) (((hash >> 24) & 0xFFFFFFL) - 0x7FFFFFL);
    }

    public static int getY(long hash) {
        return (int) ((hash >> 48) & 0x1FFL);
    }

    public static int getFlags(long hash) {
        return (int) ((hash >> (48 + 9)) & 0b111);
    }
    
    public static void main(String[] args) {
//        short[] data = new short[] { (short) 0b1010101010101010, (short)0b1100110011001100 };
//        int idata = data[0] | data[1] << 16;
//        System.out.println(Integer.toBinaryString(idata));
//        long l = toHash(0, 0, 0, 3);
//        System.out.println(Integer.toBinaryString(getFlags(l)));
//        l = 3L<<32;
//        int a = (int) ((l>>32)&3);
//        System.out.println(Long.toBinaryString(l));
//        System.out.println(Integer.toBinaryString(a));
        int l1 = 15<<8|3;
        int l2 = 12<<8|12;
        int l3 = (l1+l2) >> 1;
        int l4 = (l3>>8)&15;
        int l5 = l3&15;
        System.out.println(l4+"/"+l5);
    }

}
