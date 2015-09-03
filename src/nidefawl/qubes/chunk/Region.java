package nidefawl.qubes.chunk;

import nidefawl.qubes.render.region.MeshedRegion;

public class Region {
    public static final int REGION_SIZE_BITS      = 1;
    public static final int REGION_SIZE           = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK      = REGION_SIZE - 1;
    public static final int REGION_SIZE_BLOCK_SIZE_BITS    = Chunk.SIZE_BITS+REGION_SIZE_BITS;
    public static final int REGION_SIZE_BLOCKS    = 1 << REGION_SIZE_BLOCK_SIZE_BITS;
    public static final int SLICE_HEIGHT_BLOCK_BITS = 5;
    public static final int SLICE_HEIGHT_BLOCKS    = 1<<SLICE_HEIGHT_BLOCK_BITS;
    
    public static final int STATE_INIT            = 0;
    public static final int STATE_LOADING         = 1;
    public static final int STATE_LOAD_COMPLETE   = 2;

    public final int       rX;
    public final int       rZ;
    public final Chunk[][] chunks     = new Chunk[REGION_SIZE][REGION_SIZE];
    public long            createTime = System.currentTimeMillis();
    public int             index;

    public int          state        = STATE_INIT;
    public MeshedRegion meshedRegion = null;      //TODO keep that seperate

    public Region(int regionX, int regionZ) {
        this.rX = regionX;
        this.rZ = regionZ;
    }

    boolean isEmpty = false;

    public boolean isEmpty() {
        return isEmpty;
    }

    public int getHighestBlock() {
        int topBlock = 0;
        for (int i = 0; i < REGION_SIZE; i++) {
            for (int k = 0; k < REGION_SIZE; k++) {
                int y = chunks[i][k].getTopBlock();
                if (y > topBlock) {
                    topBlock = y;
                }
            }
        }
        return topBlock;
    }

    public void flushBlockData() {
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z].deallocate();
            }
        }
    }

    public boolean hasBlockData() {
        return !chunks[0][0].isEmpty();
    }

    public void release() {
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z] = null;
            }
        }
        state = STATE_INIT;
    }

    public void setChunk(int x, int z, Chunk c) {
        this.chunks[x][z] = c;
    }

    public final int getTypeId(int i, int j, int k) {
        
        try {

            Chunk chunk = chunks[i >> Chunk.SIZE_BITS][k >> Chunk.SIZE_BITS];
            if (chunk == null) {
                return 0;
            }
            return chunk.getTypeId(i & 0xF, j, k & 0xF);   
        } catch (Exception e) {
            System.err.println("Exception while trying to access block "+i+"/"+j+"/"+k+" in region "+this.rX+"/"+this.rZ);
            throw e;
        }
    }


    public boolean isChunkLoaded(int x, int z) {
        Chunk c = chunks[x][z];
        return c != null;
    }

    public boolean allLoadedX(int x) {
        for (int i = 0; i < Region.REGION_SIZE; i++) {
            if (!isChunkLoaded(x, i))
                return false;
        }
        return true;
    }

    public boolean allLoadedZ(int z) {
        for (int i = 0; i < Region.REGION_SIZE; i++) {
            if (!isChunkLoaded(i, z))
                return false;
        }
        return true;
    }
}
