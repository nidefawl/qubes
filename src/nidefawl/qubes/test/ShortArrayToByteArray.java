package nidefawl.qubes.test;

import java.util.Random;

import nidefawl.qubes.io.ByteArrIO;

public class ShortArrayToByteArray {

    public static void main(String[] args) {
        Random rand = new Random();
        int[] idata = new int[10];
        for (int i = 0; i < idata.length; i++) {
            int nr = rand.nextInt(Short.MAX_VALUE * 2);
            idata[i] = nr;
            System.out.print(nr + " ");
        }
        System.out.println();
        short[] data = new short[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (short) idata[i];
            int bla = data[i] & 0xFFFF;
            System.out.print(bla + " ");
        }
        System.out.println();
        byte[] b = ByteArrIO.shortToByteArray(data);
        short[] s = ByteArrIO.byteToShortArray(b);
        for (int i = 0; i < s.length; i++) {
            int bla = s[i] & 0xFFFF;
            System.out.print(bla + " ");
        }
        System.out.println();

    }
}
