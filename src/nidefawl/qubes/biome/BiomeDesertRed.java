/**
 * 
 */
package nidefawl.qubes.biome;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BiomeDesertRed extends BiomeDesert {
    public BiomeDesertRed(int i) {
        super(i);
        colorGrass = 0x6D9133;
        colorLeaves = 0x6D9133;
        colorFoliage = 0x6D9133;
    }
    

    public Block getStone() {
        return Block.stones.sandstone_red;
    }
    public Block getTopBlock() {
        return Block.sand_red;
    }
    public Block getSoilBlock() {
        return Block.stones.sandstone_red;
    }
}
