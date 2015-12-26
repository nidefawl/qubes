/**
 * 
 */
package nidefawl.qubes.block;

import java.util.ArrayList;
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
            String name = block.getName()+"_stairs";
            if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
                for (int i = 0; i < block.textures.length; i++) {
                    BlockStairs slab = new BlockStairs(name, block, i);
                    slab.setCategory(BlockCategory.STONE);
                    addBlock(block);
                }
            } else {
                BlockStairs slab = new BlockStairs(name, block);
                slab.setCategory(BlockCategory.STONE);
                addBlock(block);
            }
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }

}
