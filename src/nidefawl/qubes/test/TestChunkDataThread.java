/**
 * 
 */
package nidefawl.qubes.test;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import nidefawl.qubes.chunk.*;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class TestChunkDataThread implements Runnable {

    static int          iterations  = 5;
    static int SLICED = 0;
    final static int          NUM_THREADS = 8;
    volatile static ChunkData current     = null;
    static CyclicBarrier      barrier;
    

    static ChunkData newData() {
        if (SLICED == 0) {
            return new ChunkDataSliced();
        }
        if (SLICED == 1) {
            return new ChunkDataSliced2();
        }
        return new ChunkDataFull();
    }

    protected boolean running;
    
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++)
        {
            String ti = "Sliced"+i;
            if (i == 2)
                ti = "Full";
            System.out.println("RUNNING "+ti+" TEST");
            SLICED = i;
            current = newData();
            iterations = 10000;
            test1(current);
            iterations = 5;
            current = newData();
            try {
                Thread.sleep(44);
            } catch (Exception e) {
                
            }
            final TestChunkDataThread[] threads = new TestChunkDataThread[NUM_THREADS];
            final int[] cnt = new int[] {0};
            barrier = new CyclicBarrier(NUM_THREADS, new Runnable() {
                public void run() {
//                    System.out.println("barrier reached "+Thread.currentThread());
                    current = newData();
                    cnt[0]++;
                    if (cnt[0] > 1100) {
                        for (int a = 0; a < NUM_THREADS; a++)
                            threads[a].running = false;
                    }
                    try {
                        Thread.sleep(4);
                    } catch (Exception e) {
                        
                    }
                }
            });
            for (int a = 0; a < NUM_THREADS; a++) {
                threads[a] = new TestChunkDataThread();
            }
            for (int a = 0; a < NUM_THREADS; a++)
                threads[a].startThread();

            long time = 0;
            for (int a = 0; a < NUM_THREADS; a++) {
                threads[a].joinThread();
                time += threads[a].time/1000;
            }
            System.out.println(ti+" TIME "+(time/1000)+"ms");
            if (i == 0)
                System.out.println("RACE_LOOPS "+ChunkDataSliced.RACE_LOOPS);
        }
    }

    /**
     * 
     */
    Thread thread;
    private void joinThread() {
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void startThread() {
        running = true;
        this.thread.start();
        
    }

    private long time;

    public TestChunkDataThread() {
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        while (running) {
            long start = System.nanoTime();
            test1(current);
            this.time += System.nanoTime() - start;
            
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                return;
            }
        }
    }

    /**
     * 
     */
    private static void test2(ChunkData d) {
        Random rand = new Random();
        long l = 0;
        long l2 = 0;
        for (int i = 0; i < iterations; i++) {
            int x = rand.nextInt(Chunk.SIZE);
            int y = rand.nextInt(World.MAX_WORLDHEIGHT);
            int z = rand.nextInt(Chunk.SIZE);
            int randomValue = rand.nextInt(1 << Byte.SIZE);
            d.setUpper(x, y, z, randomValue);
            int sLSB = d.get(x, y, z) & 0xFF;
            int sMSB = (d.get(x, y, z) >> 8) & 0xFF;
            if (sLSB != 0) {
                throw new RuntimeException("expected LSB to be zero (found " + sMSB + ")");
            }
            if (sMSB != randomValue) {
                throw new RuntimeException("expected read write to be equal (" + sMSB + " != " + randomValue + ")");
            }
            l += sMSB;
            l2 += randomValue;

        }
        if (l != l2) {
            throw new RuntimeException("expected read write to be equal (" + l + " != " + l2 + ")");
        }

    }

    /**
     * @param d
     * 
     */
    private static void test1(ChunkData d) {
        Random rand = new Random();
        long l = 0;
        long l2 = 0;
        for (int i = 0; i < iterations; i++) {
            int x = rand.nextInt(Chunk.SIZE);
            int y = rand.nextInt(World.MAX_WORLDHEIGHT);
            int z = rand.nextInt(Chunk.SIZE);
            int randomValue = rand.nextInt(1 << Byte.SIZE);
            d.setLower(x, y, z, randomValue);
            int sLSB = d.get(x, y, z) & 0xFF;
            int sMSB = (d.get(x, y, z) >> 8) & 0xFF;
            if (sMSB != 0) {
                throw new RuntimeException("expected MSB to be zero (found " + sMSB + ")");
            }
            l += sLSB;
            l2 += randomValue;

        }
        if (l != l2) {
            throw new RuntimeException("expected read write to be equal");
        }

    }
}
