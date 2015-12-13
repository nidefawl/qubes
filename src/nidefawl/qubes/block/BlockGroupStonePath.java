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
public class BlockGroupStonePath extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupStonePath(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        for (String s : stoneNames) {
            Block block = new Block(-1);
            block.setName(s+" stone path");
            block.setTextures("stones/stonepath_"+s);
            block.setCategory(BlockCategory.STONE);
            addBlock(block);
        }
        
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }

}
