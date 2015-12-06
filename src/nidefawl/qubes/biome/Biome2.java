/**
 * 
 */
package nidefawl.qubes.biome;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Biome2 extends Biome {
    public Biome2(int i) {
        super(i);
    }

    /**
     * @return
     */
    public int getStone() {
        return Block.sandstone.id;
    }
}
