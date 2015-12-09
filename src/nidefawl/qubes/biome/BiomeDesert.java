/**
 * 
 */
package nidefawl.qubes.biome;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BiomeDesert extends Biome {
    public BiomeDesert(int i) {
        super(i);
        colorGrass = 0x6D9133;
        colorLeaves = 0x6D9133;
        colorFoliage = 0x6D9133;
    }
    

    public Block getStone() {
        return Block.sandstone;
    }
    public Block getTopBlock() {
        return Block.sand;
    }
    public Block getSoilBlock() {
        return Block.sandstone;
    }
}
