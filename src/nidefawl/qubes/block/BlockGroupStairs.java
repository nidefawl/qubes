/**
 * 
 */
package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGroupStairs extends BlockGroup {
    
    final List<String> stoneNames = Lists.newArrayList();
    
    public BlockGroupStairs(BlockGroup... b) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            BlockGroup group = b[i];
            stoneNames.addAll(group.getNames());
            blocks.addAll(group.getBlocks());
        }
        for (Block block : blocks) {
            if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
                for (int i = 0; i < block.textures.length; i++) {
                    BlockStairs slab = new BlockStairs(-1, block, i);
                    slab.setName(block.getName()+" stairs");
                    addBlock(block);
                }
            } else {
                BlockStairs slab = new BlockStairs(-1, block);
                slab.setName(block.getName()+" stairs");
                addBlock(block);
            }
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }

}
