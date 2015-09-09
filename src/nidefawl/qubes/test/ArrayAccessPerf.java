/**
 * 
 */
package nidefawl.qubes.test;

import java.util.Random;

import nidefawl.qubes.perf.TimingHelper;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ArrayAccessPerf {
    static class MyEntry {
        int aNumber;
        int x, y, z;
        /**
         * 
         */
        public MyEntry(int x, int y, int z, int aNumber) {
            this.x = x; this.y = y; this.z = z; this.aNumber = aNumber;
        }
    }
    static abstract class MyArray {

        public abstract MyEntry get(int xC, int yC, int zC);
        
        public abstract void set(int xC, int yC, int zC, MyEntry myEntry);
        
    }
    static class MyArrayMult extends MyArray {
        final MyEntry[][][] multiDim = makeArrayMultDim(WIDTH,HEIGHT,LENGTH);

        public MyEntry get(int x, int y, int z) {
            return multiDim[x][z][y];
        }

        public void set(int x, int y, int z, MyEntry myEntry) {
            multiDim[x][z][y] = myEntry;
        }
        
    }
    static class MyArrayFlat extends MyArray {
        final MyEntry[] flat = makeArrayFlat(WIDTH,HEIGHT,LENGTH);

        public MyEntry get(int x, int y, int z) {
            return this.flat[y*WIDTH*LENGTH+z*WIDTH+x];
        }

        public void set(int x, int y, int z, MyEntry myEntry) {
            this.flat[y*WIDTH*LENGTH+z*WIDTH+x] = myEntry;
        }
    }

    final static int WIDTH = 16;
    final static int LENGTH = 16;
    final static int HEIGHT = 16;
    MyArrayFlat f = new MyArrayFlat();
    MyArrayMult m = new MyArrayMult();

    
    
    public static void main(String[] args) {
        ArrayAccessPerf perf = new ArrayAccessPerf();
        perf.run();
    }


    /**
     * 
     */
    private void run() {
        Random rand = new Random(0xDeadbeef);
        int a = calc(m, rand);
        Random rand2 = new Random(0xDeadbeef);
        int b = calc(f, rand2);
        for (int i = 0; i < 200; i++) {
            a += calc(m, rand);
            b += calc(f, rand2);
        }
        TimingHelper.startSilent(0);
        for (int i = 0; i < 13200; i++) {
            a += calc(m, rand);
        }
        long l1 = TimingHelper.stopSilent(0);
        System.out.println(l1);
        TimingHelper.startSilent(1);
        for (int i = 0; i < 13200; i++) {
            b += calc(f, rand2);
        }
        long l2 = TimingHelper.stopSilent(1);
        System.out.println(l2);
        System.out.println(a+","+b);
    }


    private int calc(MyArray arr, Random rand) {
        {
            int nr = 0;
            for (int t = 0; t < 244; t++) {
                int xC = rand.nextInt(16);
                int yC = rand.nextInt(16);
                int zC = rand.nextInt(16);
                arr.set(xC,yC,zC, new MyEntry(xC, yC, zC, rand.nextInt()));
            }
            for (int t = 0; t < 44; t++) {
                int xC = rand.nextInt(16);
                int yC = rand.nextInt(16);
                int zC = rand.nextInt(16);
                MyEntry e = arr.get(xC,yC,zC);
                nr += e.aNumber;
                e.aNumber+=2;
            }
            for (int t = 0; t < 244; t++) {
                int xC = rand.nextInt(16);
                int yC = rand.nextInt(16);
                int zC = rand.nextInt(16);
                arr.set(xC,yC,zC, new MyEntry(xC, yC, zC, rand.nextInt()));
            }
            for (int t = 0; t < 133; t++) {
                int xC = rand.nextInt(16);
                int yC = rand.nextInt(16);
                int zC = rand.nextInt(16);
                MyEntry e = arr.get(xC,yC,zC);
                nr += e.aNumber;
                e.aNumber+=2;
            }
            return nr;
        }
    }



    /**
     * @param i
     * @param j
     * @param k
     * @return
     */
    private static MyEntry[][][] makeArrayMultDim(int i, int j, int k) {
        
        MyEntry[][][] xDim = new MyEntry[i][][];
        int idx = 0;
        for (int x = 0; x < i; x++) {
            MyEntry[][] zDim = xDim[x] = new MyEntry[k][];
            for (int z = 0; z < k; z++) {
                MyEntry[] yDim = zDim[z] = new MyEntry[j];
                for (int y = 0; y < j; y++) {
                    yDim[y] = new MyEntry(x, y, z, idx++);
                }
            }
        }
        return xDim;
    }
    private static MyEntry[] makeArrayFlat(int w, int h, int l) {
        MyEntry[] flat = new MyEntry[w*h*l];
        int idx = 0;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < l; z++) {
                for (int y = 0; y < h; y++) {
                    flat[y*(w*l)+z*w+x] = new MyEntry(x, y, z, idx++);
                }
            }
        }
        return flat;
    }

}
