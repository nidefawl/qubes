package nidefawl.qubes.world;

import java.util.HashSet;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator;

public class World {
    public static final float MAX_XZ = RegionLoader.MAX_REGION_XZ*Region.REGION_SIZE*Chunk.SIZE;
    public static final float MIN_XZ = -MAX_XZ;
    HashSet<Entity>      entities = new HashSet<>();

    public final int     worldHeight;
    public final int     worldHeightMinusOne;
    public final int     worldHeightBits;
    public final int     worldHeightBitsPlusFour;
    public final int     worldSeaLevel;

    private long         seed;

    private AbstractGen generator;
    public int worldId;
    private int dayLen=1000;
    private int time;

    private final RegionLoader regionLoader;
    public static final int MAX_WORLDHEIGHT = 256;

    public World(int worldId, long seed, RegionLoader regionLoader) {
        this.regionLoader = regionLoader;
        this.seed = seed;
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
        this.generator = new TerrainGenerator(this, this.seed);
        
    }

    public Chunk generateChunk(int i, int j) {
        return this.generator.generateChunk(i, j);
    }
    public float getSunAngle(float fTime) {
        int timeOffset = this.time%dayLen;
        float fSun = (timeOffset+fTime)/(float) dayLen + 0.25F;
        if (fSun<0)fSun++;
        if (fSun>1)fSun--;
        float f = 1.0F - (float)(Math.cos(fSun*Math.PI)+1)/2.0F;
//        return fSun+(f-fSun)/3.0F;
        return 0.89F;
    }

    public void tickUpdate() {
        this.time++;
//        int offset = time%dayLen;
//        if (offset < dayLen/3) {
//            time += dayLen/3;
//        }
    }
    public int getType(int x, int y, int z) {
        Chunk c = getChunk(x>>4, z>>4);
        if (c == null) {
            return 0;
        }
        return c.getTypeId(x&0xF, y, z&0xF);
    }
    public boolean setType(int x, int y, int z, int type, int render) {
        Chunk c = getChunk(x>>4, z>>4);
        if (c == null) {
            return false;
        }
        c.setType(x&0xF, y, z&0xF, type);
        if ((render & Flags.RENDER) != 0) {
            regionLoader.flagBlock(x, y, z);
        }
        return true;
    }
    
    public Chunk getChunk(int x, int z) {
        return regionLoader.get(x, z);
    }
    
    
}
