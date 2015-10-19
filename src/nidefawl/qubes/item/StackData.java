/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.network.StreamIO;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class StackData implements StreamIO {
    public final static int HAS_BLOCK_DATA = 1;
    BlockData data;

    public StackData() {
    }
    public StackData(StackData stackData) {
        if (stackData.data != null)
            this.data = stackData.data.copy();
    }
    public void setBlockData(BlockData data) {
        this.data = data;
    }
    public BlockData getBlockData() {
        return this.data;
    }

    @Override
    public void read(DataInput in) throws IOException {
        int flags = in.readInt();
        if ((flags & HAS_BLOCK_DATA) != 0) {
            int type = in.readUnsignedByte();
            int len = in.readUnsignedByte()*4;
            BlockData bdata = BlockData.fromType(type);
            this.data = bdata;
            if (bdata != null) {
                byte[] data = new byte[len];
                in.readFully(data);
                bdata.readData(data, 0);    
            } else
                in.skipBytes(len);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        int flags = 0;
        if (this.data != null) {
            flags|=HAS_BLOCK_DATA;
        }
        out.writeInt(flags);
        if (this.data != null) {
            out.writeByte(this.data.getTypeId());
            int l = this.data.getLength();
            out.writeByte(l/4);
            this.data.writeDataToStream(out);
        }
    }
    
    public StackData copy() {
        return new StackData(this);
    }

}
