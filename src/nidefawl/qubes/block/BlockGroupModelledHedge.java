/**
 * 
 */
package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGroupModelledHedge extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupModelledHedge(BlockGroup... b) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            BlockGroup group = b[i];
            stoneNames.addAll(group.getNames());
            blocks.addAll(group.getBlocks());
        }
        HashMap<String, String> modelTypes = Maps.newHashMap();
//        modelTypes.put("smooth", "models/block_smooth.qmodel");
        modelTypes.put("hedge", "models/block_hedge.qmodel");
//        modelTypes.put("hedge_l", "models/block_hedge_l.qmodel");
//        modelTypes.put("hedge_t", "models/block_hedge_t.qmodel");
//        modelTypes.put("hedge_x", "models/block_hedge_x.qmodel");
        
        for (Entry<String, String> entry : modelTypes.entrySet()) {
            for (Block block : blocks) {
                String name = block.getName()+"_"+entry.getKey();
                if (block.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
                    for (int i = 0; i < block.textures.length; i++) {
                        BlockHedgeModelled blockModelled = new BlockHedgeModelled(name, block, i);
                        blockModelled.setCategory(BlockCategory.LEAVES);
                        blockModelled.setModels(new String[] { entry.getValue() });
                        addBlock(blockModelled);
                    }
                } else {
                    BlockHedgeModelled blockModelled = new BlockHedgeModelled(name, block);
                    blockModelled.setCategory(BlockCategory.LEAVES);
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
