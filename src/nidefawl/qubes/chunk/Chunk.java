package nidefawl.qubes.chunk;

import java.util.Arrays;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.blockdata.BlockDataSliced;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.world.World;

public class Chunk {
    public static final int SIZE_BITS       = 4;
    public static final int SIZE            = 1 << SIZE_BITS;
    public static final int MASK            = SIZE - 1;
    public static final int DATA_BITS = (1<<8)-1;
    public World                  world;
    public final int              x;
    public final int              z;
    public final int              worldHeightBits;
    private final int             height;
    public final short[]          blocks; //TODO: split up in height slices to reduce memory usage of air space
    public final ChunkDataSliced2 blockMetadata;
    public final BlockDataSliced  blockData;
    public final byte[]           blockLight;
    public final int[]            heightMap       = new int[SIZE * SIZE];
    public final byte[]           biomes          = new byte[SIZE * SIZE];
    public int                    facesRendered;
    public long                   loadTime        = System.currentTimeMillis();
    boolean                       updateHeightMap = true;
    public boolean                needsSave       = false;
    boolean                       isEmpty         = false;
    private int                   top;
    public boolean                isLit           = false;
    public boolean                isValid         = true;
    public boolean                isUnloading     = false;
    public boolean                isPopulated     = false;

    public Chunk(World world, int x, int z, int heightBits) {
        this.blockLight = new byte[1 << (heightBits + SIZE_BITS * 2)];
        this.blocks = new short[1 << (heightBits + SIZE_BITS * 2)];
        this.worldHeightBits = heightBits;
        this.height = 1 << this.worldHeightBits;
        this.x = x;
        this.z = z;
        this.blockMetadata = new ChunkDataSliced2();
        this.blockData = new BlockDataSliced();
        this.world = world;
    }

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
        return isEmpty || blocks == null;
    }

    public int getBlockX() {
        return this.x << SIZE_BITS;
    }

    public int getBlockZ() {
        return this.z << SIZE_BITS;
    }

    public short[] g() {
        return this.blocks;
    }
    public int getTypeId(int i, int j, int k) {
        return this.blocks[j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i] & Block.BLOCK_MASK;
    }

    /**
     * @param i
     * @param j
     * @return
     */
    public Biome getBiome(int i, int j) {
        return Biome.get(this.biomes[j<<SIZE_BITS|i]&0xFF);
    }

    public BlockData getBlockData(int i, int j, int k) {
        return this.blockData.get(i, j, k);
    }
    public int getData(int i, int j, int k) {
        return this.blockMetadata.get(i, j, k)&DATA_BITS;
    }

    /**
     * @param x2
     * @param y
     * @param z2
     * @return
     */
    public short getFullData(int i, int j, int k) {
        return this.blockMetadata.get(i, j, k);
    }

    public boolean setFullData(int i, int j, int k, short data) {
        return this.blockMetadata.set(i, j, k, data);
    }
    
    public boolean setData(int i, int j, int k, int data) {
        return this.blockMetadata.setLower(i, j, k, data);
    }
    public boolean setBlockData(int i, int j, int k, BlockData bd) {
        return this.blockData.set(i, j, k, bd);
    }

    public int getTopBlock(int i, int k) {
        int y = 1 << worldHeightBits;
        while (y > 0) {
            int block = this.blocks[--y << (SIZE_BITS * 2) | k << (SIZE_BITS) | i]&Block.BLOCK_MASK;
            if (block > 0 && (Block.isOpaque(block) || block == Block.water.id)) {
                return y;
            }
        }
        return -1;
    }
    /**
     * @param i
     * @param y
     * @param j
     * @param type
     * @param data
     * @return
     */
    public boolean setTypeData(int i, int j, int k, int type, int data) {
        int xz = k << (SIZE_BITS) | i;
        int idx = j << (SIZE_BITS * 2) | xz;
        int cur = this.blocks[idx] & Block.BLOCK_MASK;
        int dataV = this.blockMetadata.get(i, j, k) & DATA_BITS;
        if (cur == type && dataV == data) {
            return false;
        }
        boolean b = false;
        if (dataV != data) {
            this.blockMetadata.setLower(i, j, k, data);
            b = true;
        }
        if (cur != type) {
            this.blocks[idx] = (short) type;
            int curHeight = heightMap[xz];
            if (j >= curHeight - 1) {
                updateHeightMap(i, k);
            }
            b = true;
        }
        if (b) {
            flagModified();
            return true;
        }
        return false;
    }

    public boolean setType(int i, int j, int k, int type) {
        int xz = k << (SIZE_BITS) | i;
        int idx = j << (SIZE_BITS * 2) | xz;
        int cur = this.blocks[idx] & Block.BLOCK_MASK;
        if (cur != type) {
            this.blocks[idx] = (short) type;
            this.blockMetadata.setLower(i, j, k, 0);
            int curHeight = heightMap[xz];
            if (j >= curHeight-1) {
                updateHeightMap(i, k);
            }
            flagModified();
            return true;
        }
        return false;
    }

    public void flagModified() {
        this.updateHeightMap = true;
        this.needsSave = true;
    }

    public int getTopBlock() {
        if (this.updateHeightMap || this.top < 0) {
            updateChunk();
        }
        return top;
    }

    private void updateChunk() {
        this.updateHeightMap = false;
        int topY = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int k = 0; k < SIZE; k++) {
                int y = getTopBlock(i, k);
                if (y > topY) {
                    topY = y;
                }
            }
        }
        this.top = topY;
    }


    public boolean justLoaded() {
        return System.currentTimeMillis() - this.loadTime < 10000;
    }

    public short[] getBlocks() {
        return this.blocks;
    }

    public byte[] getBlockLight() {
        return blockLight;
    }

    public int getLight(int i, int j, int k, int type) {
        if (j >= this.height) {
            return type == 1 ? 0xF : 0;
        }
        if (j < 0) {
            return 0;
        }
        int idx = j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i;
        byte light = this.blockLight[idx];
        if (type == 1) {
            light >>= 4;
        }
        return light & 0xF;
    }


    public int getLight(int i, int j, int k) {
        if (j >= this.height) {
            return 0xF0;
        }
        if (j < 0) {
            return 0;
        }
        int idx = j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i;
        byte light = this.blockLight[idx];
        return light&0xFF;
    }

    public boolean setLight(int i, int j, int k, int type, int val) {
        int idx = j << (SIZE_BITS * 2) | k << (SIZE_BITS) | i;
        byte light = this.blockLight[idx];
        boolean changed = false;
        if (type == 1) {
            val <<= 4;
            changed = (light & 0xF0) != val;
            light = (byte) (light & 0x0F | val & 0xF0);
        } else if (type == 0) {
            changed = (light & 0x0F) != val;
            light = (byte) (light & 0xF0 | val & 0x0F);
        } else {
            changed = light != (byte) val;
            light = (byte)val;
        }
        this.blockLight[idx] = light;
        if (changed) {
            flagModified();
        }
        return changed;
    }

    public int getHeightMap(int i, int k) {
        int xz = k << (SIZE_BITS) | i;
        return heightMap[xz];
    }

    /** Called after setType if heightmap changed
     *  updates sunlight propagation */
    private void updateHeightMap(int i, int k) {
        int xz = k << (SIZE_BITS) | i;
        int prevH = heightMap[xz];
        int y = this.height - 1;
        for (; y >= 0; y--) {
            short block = y == 0 ? 1 : this.blocks[(y-1) << (SIZE_BITS * 2) | xz];
            if (block != 0) {
                heightMap[xz] = y;
                break;
            }
        }
        /*
         * 
        for (; y > 0; y--) {
            short block = y == 0 ? 1 : this.blocks[(y-1) << (SIZE_BITS * 2) | xz];
            if (block != 0||y-1==0) {
                heightMap[xz] = y-1;
                break;
            }
        }
         */
        if (prevH != y) {
            boolean add = prevH > y;
            int min = Math.min(prevH, y);
            int max = Math.max(prevH, y);
            this.world.updateLightHeightMap(this, i, k, min, max, add);
//            for (y = max; y > min; y--) {
//                this.world.updateLight(this.x<<SIZE_BITS|i, y, this.z<<SIZE_BITS|k);
//            }
        }
    }

    public void initLight() {
        //Zero out all light
        for (int i = 0, len = this.blockLight.length; i < len; i++)
            this.blockLight[i] = 0;
        
        initHeightMap();

        //Propagate sunlight down
        for (int xz = 0; xz < SIZE * SIZE; xz++) {
            for (int y = this.heightMap[xz]; y < this.height; y++) {
                this.setLight(xz & MASK, y, (xz >> SIZE_BITS) & MASK, 1, 15);
            }
        }
        this.world.flagChunkLightUpdate(x, z);
    }
    
    public void initHeightMap() {
        //Zero out height map
        Arrays.fill(this.heightMap, (int) 0);

        //Calculate new heightmap
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int y = this.height - 1;
                int xz = z << (SIZE_BITS) | x;
                for (; y > 0; y--) {
                    short block = this.blocks[(y) << (SIZE_BITS * 2) | xz];
                    if (block != 0) {
                        break;
                    }
                }
                this.heightMap[xz] = Math.min(this.height - 1, y + 1);
            }
        }
    }

    public int[] getHeightMap() {
        return heightMap;
    }
    public void preUnload() {
        this.isValid = false;
    }

    public void postLoad() {
        if (!this.isLit) {
            initLight();
        } else {
            initHeightMap();   
        }
    }

    public void postGenerate() {
        initLight();
    }

    /**
     * @param bb
     * @return 
     */
    public byte[] getLights(BlockBoundingBox bb) {
        int w = bb.getWidth();
        int h = bb.getHeight();
        int l = bb.getLength();
        int volume = w*h*l;
        if (volume <= 0) {
            throw new IllegalArgumentException("Expected bb volume to be in range. (Volume is "+volume+"). "+bb.toString());
        }
        byte[] lightData = new byte[volume];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < l; z++) {
                int xz = (z+bb.lowZ)<<(SIZE_BITS)|(x+bb.lowX);
                for (int y = 0; y < h; y++) {
                    int idx = (y+bb.lowY)<<(SIZE_BITS*2)|xz;
                    lightData[y*w*l+z*w+x] = this.blockLight[idx];
                }
            }
        }
        return lightData;
    }

    /** 
     * @param decompressed
     * @param box
     * @return 
     */
    public boolean setLights(byte[] lightData, BlockBoundingBox bb) {
        int w = bb.getWidth();
        int h = bb.getHeight();
        int l = bb.getLength();
        int volume = w*h*l;
        if (volume <= 0) {
            throw new IllegalArgumentException("Expected bb volume to be in range. (Volume is "+volume+"). "+bb.toString());
        }
        boolean changed = false;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < l; z++) {
                int xz = (z+bb.lowZ)<<(SIZE_BITS)|(x+bb.lowX);
                for (int y = 0; y < h; y++) {
                    int idx = (y+bb.lowY)<<(SIZE_BITS*2)|xz;
                    byte cur = this.blockLight[idx];
                    byte newV = lightData[y*w*l+z*w+x];
                    changed |= cur!=newV;
                    this.blockLight[idx] = newV;
                }
            }
        }
        return changed;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Chunk["+this.world.getName()+","+x+","+z+"]";
    }

}