/**
 * 
 */
package nidefawl.qubes.chunk.blockdata;

import static nidefawl.qubes.block.BlockQuarterBlock.*;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockQuarterBlock;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockDataQuarterBlock extends BlockData {
    public final short[] blockIDs = new short[Q_SIZE];
    public final byte[] blockMeta = new byte[Q_SIZE];

    @Override
    protected boolean compareData(BlockData other) {
        BlockDataQuarterBlock qOther = (BlockDataQuarterBlock) other;
        for (int i = 0; i < Q_SIZE; i++) {
            if (this.blockIDs[i] != qOther.blockIDs[i])
                return false;
            if (this.blockMeta[i] != qOther.blockMeta[i])
                return false;
        }
        return true;
    }

    @Override
    public int getTypeId() {
        return BlockQuarterBlock.Q_DATA_TYPEID;
    }

    @Override
    public int getLength() {
        return 3 * blockIDs.length;
    }
    
    @Override
    public int writeData(byte[] bytes, int offset) {
        for (int i = 0; i < blockIDs.length; i++) {
            bytes[i*3+0+offset] = (byte) (blockIDs[i]&0xFF);
            bytes[i*3+1+offset] = (byte) ((blockIDs[i]>>8)&0xFF);
            bytes[i*3+2+offset] = (byte) (blockMeta[i]&0xFF);
        }
        return blockIDs.length*3;
    }

    @Override
    public int readData(byte[] out, int offset) {
        for (int i = 0; i < blockIDs.length; i++) {
            blockIDs[i] = (short) ( (out[i*3+0+offset]&0xFF) | ((out[i*3+1+offset]&0xFF)<<8) );
            blockMeta[i] = out[i*3+2+offset];
        }
        return blockIDs.length*3;
    }

    public void fillIntArr(int[] quarters) {
        for (int i = 0; i < blockIDs.length; i++) {
            quarters[i] = this.blockIDs[i] & Block.BLOCK_MASK;
        }
    }

    public void setTypeAndData(int i, int j, int k, int id, int data) {
        int idx = idx(i, j, k);
        if (id == Block.quarter.id)
            id = 0;
        this.blockIDs[idx] = (short) id;
        this.blockMeta[idx] = (byte) data;
    }
    public void setType(int i, int j, int k, int id) {
        int idx = idx(i, j, k);
        if (id == Block.quarter.id)
            id = 0;
        this.blockIDs[idx] = (short) id;
        this.blockMeta[idx] = (byte) 0;
    }
    public void setData(int i, int j, int k, int id) {
        int idx = idx(i, j, k);
        this.blockMeta[idx] = (byte) 0;
    }

    public int getType(int i, int j, int k) {
        int idx = idx(i, j, k);
        return this.blockIDs[idx] & Block.BLOCK_MASK;
    }
    public int getData(int i, int j, int k) {
        int idx = idx(i, j, k);
        return this.blockMeta[idx] & 0xFF;
    }
    

    //TODO: do not flip x!!!!
    public final static int idx(int x, int y, int z) {
        x&=1;
        y&=1;
        z&=1;
        return y*4+z*2+(z>0?1-x:x);
    }

    @Override
    public BlockData copy() {
        BlockDataQuarterBlock b = new BlockDataQuarterBlock();
        for (int i = 0; i < Q_SIZE; i++) {
            b.blockIDs[i] = this.blockIDs[i];
            b.blockMeta[i] = this.blockMeta[i];
        }
        return b;
    }

    /**
     * @return 
     * 
     */
    public boolean allFullBB() {
        for (int i = 0; i < Q_SIZE; i++) {
            Block block = Block.get(this.blockIDs[i]);
            if (!block.isFullBB())
                return false;
        }
        return true;
    }

}
