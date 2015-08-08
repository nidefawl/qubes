package nidefawl.engine.chunk;

import nidefawl.game.block.Block;


public class Chunk {
    public final int    worldHeightBits;
    public short[] blocks;
    public final int    x;
    public final int    z;
    public int facesRendered;
    public byte[] biomes = new byte[256];
    public Chunk(short[] blocks, int x, int z, int heightBits) {
        this.worldHeightBits = heightBits;
        this.blocks = blocks;
        this.x = x;
        this.z = z;
    }
    boolean isEmpty = false;
    public void checkIsEmtpy() {
        if (isEmpty) return;
        for (int a = 0; a < blocks.length; a++) {
            if (blocks[a] != 0) {
                return;
            }
        }
        isEmpty = true;
    }
    public boolean isEmpty() {
        return isEmpty;
    }
    public int getBlockX() {
        return this.x << 4;
    }
    public int getBlockZ() {
        return this.z << 4;
    }
    public int getTypeId(int i, int j, int k) {
        return this.blocks[j << (8) | k << (4) | i] & Block.BLOCK_MASK;
    }
    public int getTopBlock(int i, int k) {
        int y = 1 << worldHeightBits;
        while (y > 0) {
            if (this.blocks[--y << (8) | k << (4) | i] != 0) {
                return y;
            }
        }
        return -1;
    }
    public void setType(int i, int j, int k, int type) {
        this.blocks[j << (8) | k << (4) | i] = (short) type;
    }
    public int getTopBlock() {
        int top = 0;
        for (int i = 0; i < 16; i++) {
            for (int k = 0; k < 16; k++) {
                int y = getTopBlock(i, k);
                if (y > top)
                    top = y;
            }
        }
        return top;
    }
    public int getBiome(int i, int j, int k) {
        return this.biomes[i|k<<4]&0XFF;
    }
}