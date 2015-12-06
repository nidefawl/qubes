/**
 * 
 */
package nidefawl.qubes.biome;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Biome {
    public final static Biome[] biomes = new Biome[256];
    public final static Biome BIOME1 = new Biome1(0).setDebugColor(0x32dd32);
    public final static Biome BIOME2 = new Biome2(1).setDebugColor(0x3232dd);
    public int color;
    public int id;
    /**
     * 
     */
    public Biome(int id) {
        this.id = id;
        this.biomes[id] = this;
    }

    /**
     * @param i
     * @return
     */
    public Biome setDebugColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * @param id
     * @return
     */
    public static Biome get(int id) {
        if (id < 0 || id >= biomes.length)
            return BIOME1;
        return biomes[id];
    }

    /**
     * @return
     */
    public int getStone() {
        return Block.granite.id;
    }
}
