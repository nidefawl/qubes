package nidefawl.qubes.worldgen.terrain;

import java.util.Random;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;

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
        Chunk c = new Chunk(this.world, chunkX, chunkZ);
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
    public WorldGenInit getWorldGen(WorldServer world, long seed, WorldSettings settings) {
        WorldGenInit init = new WorldGenInit();
        init.generator = this;
        init.biomeManager = new EmptyBiomeManager(world, seed, settings);
        init.populator = new EmptyChunkPopulator(world, seed, settings);
        return init;
    }
}
