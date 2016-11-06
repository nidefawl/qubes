package nidefawl.qubes.worldgen.terrain;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;

public class TerrainGenFlatSand128 implements ITerrainGen {
    public final static String GENERATOR_NAME = "flat_sand";

    private WorldServer world;

    public TerrainGenFlatSand128(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(this.world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk, short[] blocks) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128d; y++) {
                    blocks[y << 8 | z << 4 | x] = (short) Block.sand.id;
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
