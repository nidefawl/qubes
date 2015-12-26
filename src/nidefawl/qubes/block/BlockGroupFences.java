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
public class BlockGroupFences extends BlockGroup {
    
    final List<String> stoneNames = Lists.newArrayList();
    
    public BlockGroupFences(BlockGroup... b) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            BlockGroup group = b[i];
            stoneNames.addAll(group.getNames());
            blocks.addAll(group.getBlocks());
        }
        for (Block block : blocks) {
//            if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
//                for (int i = 0; i < block.textures.length; i++) {
//                    BlockFence slab = new BlockFence(-1, block, i);
//                    slab.setName(block.getName()+" slab");
//                    slab.setCategory(BlockCategory.STONE);
//                    addBlock(slab);
//                }
//            } else {
                BlockFence slab = new BlockFence(block.getName()+"_fence", block);
                slab.setCategory(BlockCategory.UNASSIGNED);
                addBlock(slab);
//            }
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
