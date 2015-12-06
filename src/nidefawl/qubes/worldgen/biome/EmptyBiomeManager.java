/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class EmptyBiomeManager implements IBiomeManager {
    public EmptyBiomeManager(WorldServer world, long seed, WorldSettings settings) {
    }

    @Override
    public Biome getBiome(int x, int z) {
        return Biome.BIOME1;
    }
}
