package nidefawl.qubes.worldgen;

import nidefawl.qubes.world.biomes.IBiomeManager;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

public class WorldGenInit {

    public ITerrainGen generator;
    public IBiomeManager biomeManager;
    public IChunkPopulator populator;

    public ITerrainGen getGenerator() {
        return generator;
    }

    public IBiomeManager getBiomeManager() {
        return biomeManager;
    }

    public IChunkPopulator getPopulator() {
        return populator;
    }

}
