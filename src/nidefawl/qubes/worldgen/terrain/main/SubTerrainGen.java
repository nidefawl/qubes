/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import nidefawl.qubes.world.biomes.HexBiome;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class SubTerrainGen {
    public abstract class SubTerrainData {
        
    }

    public abstract SubTerrainData prepare(int cX, int cZ, HexBiome b);

    /**
     * @param hex
     * @param data
     * @param d2 TODO
     * @param blockX
     * @param blockZ
     */
    public abstract double generate(int cX, int cZ, int x, int y1, int y2, int z, HexBiome hex, SubTerrainData data, double[] d, double[] d2);
}
