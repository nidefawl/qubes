/**
 * 
 */
package nidefawl.qubes.chunk.blockdata;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.ByteArray;
import nidefawl.qubes.nbt.Tag.TagList;
import nidefawl.qubes.util.ByteArrIO;
import nidefawl.qubes.util.ByteInStream;
import nidefawl.qubes.util.ByteOutStream;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockDataSliced {
    public final static int   DATA_HEIGHT_SLICES     = World.MAX_WORLDHEIGHT >> (Chunk.SIZE_BITS);
    final static int          DATA_HEIGHT_SLICE_MASK = DATA_HEIGHT_SLICES - 1;
    final static int          SLICE_SIZE             = 1 << (Chunk.SIZE_BITS * 3);
    final static int          SLICE_SIZE_MASK        = SLICE_SIZE-1;
    final BlockData[][] array                  = new BlockData[DATA_HEIGHT_SLICES][];

    public BlockDataSliced() {
    }

    public BlockData get(int i, int j, int k) {
        int idx = j >> Chunk.SIZE_BITS;
        BlockData[] slice = getArray(idx, false);
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        return slice == null ? null : slice[slice_idx];
    }

    public boolean set(int i, int j, int k, BlockData data) {
        int idx = j >> Chunk.SIZE_BITS;
        BlockData[] slice = getArray(idx, data != null);
        if (data == null && slice == null)
            return false;
        int slice_idx = ((j & DATA_HEIGHT_SLICE_MASK) << (Chunk.SIZE_BITS * 2)) | (k << Chunk.SIZE_BITS) | i;
        BlockData v = slice[slice_idx];
        slice[slice_idx] = data;
        return BlockData.isEqual(v, data);
    }
    
    public BlockData[] getArray(int idx, boolean create) {
        if (array[idx] == null && create) {
            synchronized (array) {
                if (array[idx] == null) {
                    array[idx] = new BlockData[SLICE_SIZE];
                }
            }
        }
        return array[idx];
    }

    /**
     * @return
     * @throws IOException 
     */
    public Tag.Compound writeToTag() throws IOException {
        Tag.TagList l = null;
        for (int i = 0; i < DATA_HEIGHT_SLICES; i++) {
            BlockData[] data = getArray(i, false);
            if (data != null) {
                byte[] output = sliceToBytes(data, null, 0);
                if (output != null) {
                    Tag.Compound cmp = new Tag.Compound();
                    cmp.setByte("y", i);
                    cmp.setByteArray("data", output);
                    if (l == null) {
                        l = new TagList();
                    }
                    l.add(cmp);   
                }
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
     * @param data
     * @return
     */
    public final static byte[] sliceToBytes(BlockData[] data, byte[] output, int out_offset) {
        int n = 0;
        int len = 0;
        for (int a = 0; a < data.length; a++) {
            if (data[a] != null) {
                n++;
                len += data[a].getLength();
            }
        }
        if (n == 0)
            return null;
        len += BlockData.HEADER_SIZE*n;
        if (output == null)
            output = new byte[len+2];
        int offset = out_offset + ByteArrIO.writeShort(output, out_offset, n);
        for (int a = 0; a < data.length; a++) {
            if (data[a] != null) {
                offset += ByteArrIO.writeShort(output, offset, a);
                offset += data[a].writeHeader(output, offset);
                offset += data[a].writeData(output, offset);
            }
        }
        return output;
    }

    /**
     * @param list
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
                        BlockData[] sData = getArray(y, true);
                        byte[] bytes = bData.getArray();
                        sliceFromBytes(bytes, sData);
                    }
                }
            }
        }
    }

    /**
     * @param bytes
     * @param sData
     */
    public final static void sliceFromBytes(byte[] bytes, BlockData[] sData) {
        int numElements = ByteArrIO.readShort(bytes, 0);
        int offset = 2;
        for (int i = 0; i < numElements; i++) {
            int idx = ByteArrIO.readShort(bytes, offset);
            offset+=2;
            int type = ByteArrIO.readUnsignedByte(bytes, offset);
            offset+=1;
            int len = ByteArrIO.readUnsignedByte(bytes, offset)*4;
            offset+=1;
            BlockData bdata = BlockData.fromType(type);
            if (bdata == null) {
                offset += len;
                continue;
            }
            bdata.readData(bytes, offset);       
            offset+=len;
            sData[idx] = bdata;
        }
    }

    /**
     * 
     */
    public BlockData[][] getArrays() {
        final BlockData[][] array = new BlockData[DATA_HEIGHT_SLICES][];
        for (int i = 0; i < DATA_HEIGHT_SLICES; i++) {
            array[i] = this.array[i];
        }
        return array;
    }

}
