package nidefawl.qubes.world;

import java.util.HashSet;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RegionRenderThread;
import nidefawl.qubes.worldgen.TerrainGenerator;

public class World {
    HashSet<Entity>      entities = new HashSet<>();

    public final int     worldHeight;
    public final int     worldHeightMinusOne;
    public final int     worldHeightBits;
    public final int     worldHeightBitsPlusFour;
    public final int     worldSeaLevel;

    private long         seed;

    public RegionRenderThread loader;

    private TerrainGenerator generator;

    public World(long seed) {

        this.seed = seed;
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
        this.generator = new TerrainGenerator(this, this.seed);
        this.loader = new RegionRenderThread(this);
        
    }

    public Chunk generateChunk(int i, int j) {
        return this.generator.getChunkAt(i, j);
    }

    public void loadRegions(int x, int z, boolean follow) {
        this.loader.loadRegions(x, z, follow);
    }
}
