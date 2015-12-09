/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import nidefawl.qubes.worldgen.biome.HexBiome;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class SubTerrainGen {
    public abstract class SubTerrainData {
        
    }

    public abstract SubTerrainData prepare(int cX, int cZ);

    /**
     * @param blockX
     * @param blockZ
     * @param hex
     * @param data
     */
    public abstract double generate(int cX, int cZ, int x, int y, int z, HexBiome hex, SubTerrainData data);
}
