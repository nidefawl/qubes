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
public class BlockGroupWalls extends BlockGroup {
    
    final List<String> stoneNames = Lists.newArrayList();
    
    public BlockGroupWalls(BlockGroup... b) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            BlockGroup group = b[i];
            stoneNames.addAll(group.getNames());
            blocks.addAll(group.getBlocks());
        }
        for (Block block : blocks) {
            if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
                for (int i = 0; i < block.textures.length; i++) {
                    BlockWall slab = new BlockWall(block.getName()+"_wall", block, i);
                    slab.setCategory(BlockCategory.STONE);
                    addBlock(slab);
                }
            } else {
                BlockWall slab = new BlockWall(block.getName()+"_wall", block);
                slab.setCategory(BlockCategory.STONE);
                addBlock(slab);
            }
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
