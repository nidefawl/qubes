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
public class BlockGroupStoneBricks extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupStoneBricks(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        for (String s : stoneNames) {
            Block block = new Block(s+"_stonebrick");
            block.setTextures(
                    "stones/stonebrick_"+s+"_smooth", 
                    "stones/stonebrick_"+s+"_rough", 
                    "stones/stonebrick_"+s+"_rough_cracked", 
                    "stones/stonebrick_"+s+"_rough_cracked_mossy");
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
