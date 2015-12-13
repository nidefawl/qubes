/**
 * 
 */
package nidefawl.qubes.block;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGroupSmoothStones extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupSmoothStones(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        for (String s : stoneNames) {
            Block block = new Block(-1);
            block.setName(s+" smooth stone");
            block.setTextures("stones/stone_"+s+"_border", "stones/stone_"+s+"_smooth_border");
            block.setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
            block.setCategory(BlockCategory.STONE);
            addBlock(block);
        }
        
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }

}
