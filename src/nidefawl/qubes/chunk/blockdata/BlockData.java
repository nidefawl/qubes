/**
 * 
 */
package nidefawl.qubes.chunk.blockdata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.block.BlockOre;
import nidefawl.qubes.block.BlockQuarterBlock;
import nidefawl.qubes.util.ByteArrIO;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */ 
public abstract class BlockData {
    public final static int HEADER_SIZE = 2 /*POS*/ + 1 /*TYPE*/ + 1 /*LEN IN sizeof(int)*/;
    public static boolean isEqual(BlockData v1, BlockData v2) {
        if (v1 == v2)
            return true;
        if (v1 == null)
            return false;
        if (v2 == null)
            return false;
        return v1.compareTo(v2);
    }

    public BlockData() {
    }
    
    protected boolean compareTo(BlockData v2) {
        return this.getTypeId() == v2.getTypeId() && this.compareData(v2);
    }
    
    protected abstract boolean compareData(BlockData other);

    public abstract int getTypeId();
    public abstract int getLength(); //TODO: USE THIS ONLY FOR ALLOC, DONT WRITE IT TO STREAM (we already know the size ahead of reading from the typeid)
    
    public static BlockData fromType(int len) {
        switch (len) {
            case BlockQuarterBlock.Q_DATA_TYPEID:
                return new BlockDataQuarterBlock();
        }
        return null;
    }

    public int writeHeader(byte[] out, int offset) {
        ByteArrIO.writeByte(out, offset, getTypeId());
        offset+=1;
        int byteLen = getLength();
        if (byteLen == 0 || byteLen % 4 != 0) {
            throw new IllegalArgumentException("LENGTH MUST BE MULTIPLE OF 4");
        }
        ByteArrIO.writeByte(out, offset, byteLen / 4);
        return 2;
    }
    public abstract int writeData(byte[] out, int offset);
    public abstract int readData(byte[] out, int offset);

    public int writeDataToStream(DataOutput out) throws IOException { //TODO: IMPLEMENT THIS PERR CLASS
        byte[] data = new byte[getLength()];
        writeData(data, 0);
        out.write(data);
        return data.length;
    }

    /**
     * @param stream
     * @throws IOException 
     */
    public void readDataFromStream(DataInput stream) throws IOException { //TODO: IMPLEMENT THIS PERR CLASS
        byte[] data = new byte[getLength()];
        stream.readFully(data);
        readData(data, 0);
    }

    /**
     * @return
     */
    public abstract BlockData copy();
}
