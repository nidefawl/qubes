/**
 * 
 */
package nidefawl.qubes.chunk;

import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ChunkDataFull extends ChunkData {
    final short[]         array              = new short[Chunk.SIZE*Chunk.SIZE*World.MAX_WORLDHEIGHT];

    public ChunkDataFull() {
    }

    public short get(int i, int j, int k) {
        int slice_idx = (j << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        return array[slice_idx];
    }
    public boolean setByte(int i, int j, int k, boolean upper, int val) {
        int slice_idx = (j << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        short v = array[slice_idx];
        int MASK_UPPER = 0xFF00;
        int MASK_LOWER = 0x00FF;
        if (upper) {
            val<<=8;
            MASK_UPPER = ~MASK_UPPER;
            MASK_LOWER = ~MASK_LOWER;
        }
        short vNew = (short) (v&MASK_UPPER|val&MASK_LOWER);
        array[slice_idx] = vNew;
        return v != vNew;
    }

}
