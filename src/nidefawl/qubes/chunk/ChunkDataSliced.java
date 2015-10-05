/**
 * 
 */
package nidefawl.qubes.chunk;

import java.util.concurrent.atomic.AtomicBoolean;

import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ChunkDataSliced extends ChunkData {
    final static int        DATA_HEIGHT_SLICES = World.MAX_WORLDHEIGHT >> Chunk.SIZE_BITS;
    final static int        DATA_HEIGHT_SLICE_MASK = DATA_HEIGHT_SLICES-1;
    final static int        SLICE_SIZE = 1<<(Chunk.SIZE_BITS*3);
    final short[][]         array              = new short[DATA_HEIGHT_SLICES][];
    private AtomicBoolean[] canWrite           = new AtomicBoolean[DATA_HEIGHT_SLICES];

    public ChunkDataSliced() {
        for (int i = 0; i < DATA_HEIGHT_SLICES; i++) {
            canWrite[i] = new AtomicBoolean(false);
        }
    }

    public short get(int i, int j, int k) {
        int idx = j >> Chunk.SIZE_BITS;
        short[] slice = getArray(idx);
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        return slice[slice_idx];
    }
    public boolean setByte(int i, int j, int k, boolean upper, int val) {
        int idx = j >> Chunk.SIZE_BITS;
        short[] slice = getArray(idx);
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        short v = slice[slice_idx];
        int MASK_UPPER = 0xFF00;
        int MASK_LOWER = 0x00FF;
        if (upper) {
            val<<=8;
            MASK_UPPER = ~MASK_UPPER;
            MASK_LOWER = ~MASK_LOWER;
        }
        short vNew = (short) (v&MASK_UPPER|val&MASK_LOWER);
        slice[slice_idx] = vNew;
        return v != vNew;
    }
    
    public static int RACE_LOOPS = 0;
    private short[] getArray(int idx) {
        int loops = 0;
        while (array[idx] == null) {
            if (canWrite[idx].compareAndSet(false, true)) {
                if (array[idx] != null) {
                    throw new RuntimeException("ALREADY INSTANCIATED");
                }
                array[idx] = new short[SLICE_SIZE];
            } else {
                loops++;
                Thread.yield();
            }
        }

        RACE_LOOPS += loops;
        return array[idx];
    }

}
