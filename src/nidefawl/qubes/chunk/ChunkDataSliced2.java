/**
 * 
 */
package nidefawl.qubes.chunk;

import java.io.IOException;
import java.util.List;

import nidefawl.qubes.io.ByteArrIO;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.ByteArray;
import nidefawl.qubes.nbt.Tag.TagList;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ChunkDataSliced2 extends ChunkData {
    public final static int        DATA_HEIGHT_SLICES = World.MAX_WORLDHEIGHT >> Chunk.SIZE_BITS;
    final static int        DATA_HEIGHT_SLICE_MASK = DATA_HEIGHT_SLICES-1;
    final static int        SLICE_SIZE = 1<<(Chunk.SIZE_BITS*3);
    final short[][]         array              = new short[DATA_HEIGHT_SLICES][];

    public ChunkDataSliced2() {
    }

    public short get(int i, int j, int k) {
        int idx = j >> Chunk.SIZE_BITS;
        short[] slice = getArray(idx, false);
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        return slice == null ? 0 : slice[slice_idx];
    }

    public boolean set(int i, int j, int k, short data) {
        int idx = j >> Chunk.SIZE_BITS;
        short[] slice = getArray(idx, data != 0);
        if (data == 0 && slice == null)
            return false;
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        short v = slice[slice_idx];
        slice[slice_idx] = data;
        return v != data;
    }
    public boolean setByte(int i, int j, int k, boolean upper, int val) {
        int idx = j >> Chunk.SIZE_BITS;
        short[] slice = getArray(idx, true);
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
    
    public short[] getArray(int idx, boolean create) {
        if (array[idx] == null && create) {
            synchronized (array) {
                if (array[idx] == null) {
                    array[idx] = new short[SLICE_SIZE];
                }
            }
        }
        return array[idx];
    }

    /**
     * @return
     */
    public Tag.Compound writeToTag() {
        Tag.TagList l = null;
        for (int i = 0; i < DATA_HEIGHT_SLICES; i++) {
            short[] data = getArray(i, false);
            if (data != null) {
                if (l == null) {
                    l = new TagList();
                }
                Tag.Compound cmp = new Tag.Compound();
                cmp.setByte("y", i);
                byte[] dataBytes = ByteArrIO.shortToByteArray(data);
                cmp.setByteArray("data", dataBytes);
                l.add(cmp);
            }
        }
        if (l != null) {
            Tag.Compound cmp = new Tag.Compound();
            cmp.setByte("v", 1);
            cmp.set("list", l);
            return cmp;
        }
        return null;
    }
    /**
     * @param globalShaders
     * @throws IOException 
     */
    public void readFromTag(Tag.Compound data) throws IOException {
        if (data != null) {
            if (data.getByte("v") == 1) {
                List<? extends Tag> list = data.getList("list");
                for (Tag t : list) {
                    Tag.Compound cmp = (Tag.Compound) t;
                    int y = cmp.getByte("y")&0xFF;
                    ByteArray bData = cmp.getByteArray("data");
                    if (bData != null) {
                        byte[] bytes = bData.getArray();
                        if (bytes.length != SLICE_SIZE*2) {
                            throw new IOException("Invalid chunk data");
                        }
                        short[] sData = getArray(y, true);
                        ByteArrIO.byteToShortArray(bytes, sData);
                    }
                }
            }
        }
    }

    /**
     * 
     */
    public short[][] getArrays() {
        final short[][] array = new short[DATA_HEIGHT_SLICES][];
        for (int i = 0; i < DATA_HEIGHT_SLICES; i++) {
            array[i] = this.array[i];
        }
        return array;
    }

}
