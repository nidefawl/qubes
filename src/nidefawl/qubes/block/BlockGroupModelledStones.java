/**
 * 
 */
package nidefawl.qubes.block;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGroupModelledStones extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupModelledStones(BlockGroup... b) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            BlockGroup group = b[i];
            stoneNames.addAll(group.getNames());
            blocks.addAll(group.getBlocks());
        }
        HashMap<String, String> modelTypes = Maps.newHashMap();
//        modelTypes.put("smooth", "models/block_smooth.qmodel");
        modelTypes.put("carved", "models/block_carved.qmodel");
        
        for (Entry<String, String> entry : modelTypes.entrySet()) {
            for (Block block : blocks) {
                if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
                    for (int i = 0; i < block.textures.length; i++) {
                        BlockStoneModelled blockModelled = new BlockStoneModelled(-1, block, i);
                        blockModelled.setName(block.getName()+" "+entry.getKey());
                        blockModelled.setCategory(BlockCategory.STONE);
                        blockModelled.setModels(new String[] { entry.getValue() });
                        addBlock(blockModelled);
                    }
                } else {
                    BlockStoneModelled blockModelled = new BlockStoneModelled(-1, block);
                    blockModelled.setName(block.getName()+" "+entry.getKey());
                    blockModelled.setCategory(BlockCategory.STONE);
                    blockModelled.setModels(new String[] { entry.getValue() });
                    addBlock(blockModelled);
                }
            }
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }

}
