package nidefawl.qubes.worldgen.terrain;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.biomes.IBiomeManager;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;

public interface ITerrainGen {

    public Chunk generateChunk(int chunkX, int chunkZ);

    /**
     * @return
     */
    public Class<? extends IChunkPopulator> getPopulator();

    /**
     * @return
     */
    public Class<? extends IBiomeManager> getBiomeManager();

    /**
     * 
     */
    public void init();
}
