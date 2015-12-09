package nidefawl.qubes.worldgen.terrain;

import java.util.Random;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.worldgen.biome.EmptyBiomeManager;
import nidefawl.qubes.worldgen.biome.IBiomeManager;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;

public class TerrainGenBlockTest implements ITerrainGen {
    public final static String GENERATOR_NAME = "block_test";

    private WorldServer world;
    private long  seed;

    public TerrainGenBlockTest(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(this.world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk, short[] blocks) {
        int wh = this.world.worldHeight;
        int b = 4;
        int c = 60;
        int d = 8;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
         b = 0;
         c = 40;
         d = 2;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
    }

    @Override
    public Class<? extends IChunkPopulator> getPopulator() {
        return EmptyChunkPopulator.class;
    }

    @Override
    public Class<? extends IBiomeManager> getBiomeManager() {
        return EmptyBiomeManager.class;
    }

    @Override
    public void init() {
    }

}
