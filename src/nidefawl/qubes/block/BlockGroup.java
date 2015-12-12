/**
 * 
 */
package nidefawl.qubes.block;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class BlockGroup {

    private List<Block> blocks = Lists.newArrayList();

    void addBlock(Block block) {
        this.blocks.add(block);
    }

    public abstract List<String> getNames();
    
    public List<Block> getBlocks() {
        return this.blocks;
    }
}
