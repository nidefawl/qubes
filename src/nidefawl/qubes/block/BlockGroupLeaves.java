package nidefawl.qubes.block;

import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupLeaves extends BlockGroup {
    public final static List<String> leaveNames = Lists.newArrayList(new String[] { 
            "acacia", "birch", "cocoa", "coconut", "ebony", "mahagoni", "oak", "pine", "redwood", "spruce", "walnut", "willow"
    });

    final public Block acacia;
    final public Block birch;
    final public Block oak;
    public BlockGroupLeaves() {
        for (String s : leaveNames) {
            Block leaves = new BlockLeaves(s+"_leaves");
            leaves.setTextures("leaves/"+s);
            addBlock(leaves);
        }
        acacia = getBlocks().get(0);
        birch = getBlocks().get(1);
        oak = getBlocks().get(6);
        acacia.setTextures("leaves/acacia", "leaves/acacia_round");
        birch.setTextures("leaves/birch", "leaves/birch_round");
    }
    @Override
    public List<String> getNames() {
        return leaveNames;
    }

}
