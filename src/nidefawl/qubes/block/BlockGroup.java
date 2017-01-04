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
    static int NEXT_GROUP_ID = 0;
    final int id = NEXT_GROUP_ID++;
    public int getId() {
        return this.id;
    }

    private List<Block> blocks = Lists.newArrayList();

    void addBlock(Block block) {
        this.blocks.add(block);
        block.setBlockGroup(this);
    }

    public abstract List<String> getNames();
    
    public List<Block> getBlocks() {
        return this.blocks;
    }
    public Block getFirst() {
        return this.blocks.isEmpty() ? null : this.blocks.get(0);
    }
}
