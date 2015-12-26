package nidefawl.qubes.block;

import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupLeaves extends BlockGroup {
    final List<String> leaveNames = Lists.newArrayList(new String[] { 
            "acacia", "birch", "cocoa", "coconut", "ebony", "mahagoni", "oak", "pine", "redwood", "spruce", "walnut", "willow"
    });

    final public Block oak;
    public BlockGroupLeaves() {
        for (String s : leaveNames) {
            Block leaves = new BlockLeaves(s+"_leaves");
            leaves.setTextures("leaves/"+s);
            addBlock(leaves);
        }
        oak = getBlocks().get(6);
    }
    @Override
    public List<String> getNames() {
        return leaveNames;
    }

}
