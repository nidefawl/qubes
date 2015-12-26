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
public class BlockGroupBricks extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupBricks(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        stoneNames.add("clay");
        for (String s : stoneNames) {
            addBlock(new Block(s+"_brick").setTextures("stones/brick_"+s).setCategory(BlockCategory.STONE));
        }
        
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
