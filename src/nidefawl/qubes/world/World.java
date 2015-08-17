package nidefawl.qubes.world;

import java.util.HashSet;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator;
import nidefawl.qubes.worldgen.TestTerrain2;

public class World {
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

    public World(int worldId, long seed) {

        this.seed = seed;
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
        this.generator = new TerrainGenerator(this, this.seed);
        
    }

    public Chunk generateChunk(int i, int j) {
        return this.generator.getChunkAt(i, j);
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
    
    
}
