package nidefawl.qubes.chunk;

import nidefawl.qubes.block.Block;

public class Chunk {
    public static final int SIZE_BITS = 4;
    public static final int SIZE      = 1 << SIZE_BITS;
    public final int        worldHeightBits;
    public short[]          blocks;
    public final int        x;
    public final int        z;
    public int              facesRendered;
    public byte[]           biomes    = new byte[SIZE * SIZE];

    public Chunk(short[] blocks, int x, int z, int heightBits) {
        this.worldHeightBits = heightBits;
        this.blocks = blocks;
        this.x = x;
        this.z = z;
    }

    boolean isEmpty = false;

    public void checkIsEmtpy() {
        if (isEmpty)
            return;
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
        return this.x << SIZE_BITS;
    }

    public int getBlockZ() {
        return this.z << SIZE_BITS;
    }

    public int getTypeId(int i, int j, int k) {
        return this.blocks[j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i] & Block.BLOCK_MASK;
    }

    public int getTopBlock(int i, int k) {
        int y = 1 << worldHeightBits;
        while (y > 0) {
            if (this.blocks[--y << (SIZE_BITS * 2) | k << (SIZE_BITS) | i] != 0) {
                return y;
            }
        }
        return -1;
    }

    public void setType(int i, int j, int k, int type) {
        this.blocks[j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i] = (short) type;
    }

    public int getTopBlock() {
        int top = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int k = 0; k < SIZE; k++) {
                int y = getTopBlock(i, k);
                if (y > top)
                    top = y;
            }
        }
        return top;
    }

    public int getBiome(int i, int j, int k) {
        return this.biomes[i | k << SIZE_BITS] & 0XFF;
    }
}