/**
 * 
 */
package nidefawl.qubes.biome;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BiomeIce extends Biome {
    public BiomeIce(int i) {
        super(i);
    }
    

    @Override
    public Block getStone() {
        return Block.stones.granite;
    }
    @Override
    public Block getTopBlock() {
        return Block.snow;
    }
    @Override
    public Block getSoilBlock() {
        return Block.snow;
    }
    @Override
    public Block getWaterBlock() {
        return Block.ice;
    }
}
