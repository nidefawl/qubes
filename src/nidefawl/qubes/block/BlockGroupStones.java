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
public class BlockGroupStones extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList(new String[] { "granite", "basalt", "diorite", "marble", "sandstone", "sandstone_red" });

    final public Block granite;
    final public Block basalt;
    final public Block diorite;
    final public Block marble;
//    final public Block obsidian;
    final public Block sandstone;
    final public Block sandstone_red;
    public BlockGroupStones() {
        for (String s : stoneNames) {
            addBlock(new BlockStone(s).setTextures("rocks/"+s).setCategory(BlockCategory.ROCK));
        }
        granite = getBlocks().get(0);
        basalt = getBlocks().get(1);
        diorite = getBlocks().get(2);
        marble = getBlocks().get(3);
//        obsidian = getBlocks().get(4);
        sandstone = getBlocks().get(4);
        sandstone_red = getBlocks().get(5);
        granite.setNormalMaps("rocks/granite_normalmap");
        marble.setNormalMaps("rocks/granite_normalmap");
        sandstone.setNormalMaps("rocks/granite_normalmap");
        sandstone_red.setNormalMaps("rocks/granite_normalmap");
        basalt.setNormalMaps("rocks/basalt_normalmap");
        
    }
    @Override
    public List<String> getNames() {
        return stoneNames;
    }
}
