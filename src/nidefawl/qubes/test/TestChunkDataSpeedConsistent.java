/**
 * 
 */
package nidefawl.qubes.test;

import java.util.Random;

import nidefawl.qubes.chunk.*;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class TestChunkDataSpeedConsistent {

    final static int iterations = 10000000;

    public static void main(String[] args) {
        {

            test1(new ChunkDataSliced());
            test2(new ChunkDataSliced());
            test1(new ChunkDataFull());
            test2(new ChunkDataFull());
        }
        {

            long l = System.nanoTime();
            test1(new ChunkDataSliced());
            test2(new ChunkDataSliced());
            long took = System.nanoTime() -l;
            System.out.println(took/1000000);
        }
        {

            long l = System.nanoTime();
            test1(new ChunkDataFull());
            test2(new ChunkDataFull());
            long took = System.nanoTime() -l;
            System.out.println(took/1000000);
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
            int sLSB = d.get(x, y, z)&0xFF;
            int sMSB = (d.get(x, y, z)>>8)&0xFF;
            if (sLSB != 0) {
                throw new RuntimeException("expected LSB to be zero (found "+sMSB+")");
            }
            if (sMSB != randomValue) {
                throw new RuntimeException("expected read write to be equal ("+sMSB+" != "+randomValue+")");
            }
            l += sMSB;
            l2 += randomValue;
            
        }
        if (l != l2) {
            throw new RuntimeException("expected read write to be equal ("+l+" != "+l2+")");
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
            int sLSB = d.get(x, y, z)&0xFF;
            int sMSB = (d.get(x, y, z)>>8)&0xFF;
            if (sMSB != 0) {
                throw new RuntimeException("expected MSB to be zero (found "+sMSB+")");
            }
            l += sLSB;
            l2 += randomValue;
            
        }
        if (l != l2) {
            throw new RuntimeException("expected read write to be equal");
        }
        
    }
}
