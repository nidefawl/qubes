package nidefawl.qubes.worldgen.terrain;

import java.util.Arrays;
import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;

public class TerrainGenMines implements ITerrainGen {
    public final static String GENERATOR_NAME = "mines";

    private WorldServer world;
    private long  seed;

    public TerrainGenMines(WorldServer world, long seed, WorldSettings settings) {
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
        Arrays.fill(blocks, (short)Block.stones.getFirst().id);
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
