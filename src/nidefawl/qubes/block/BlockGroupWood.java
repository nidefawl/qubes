package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupWood extends BlockGroup {
    final List<String> woodNames = Lists.newArrayList(new String[] { 
            "acacia", "birch", "cocoa", "coconut", "ebony", "mahagoni", 
            "oak", "pine", "redwood", "spruce", "walnut", "willow"
    });

    public BlockGroupWood() {
        for (String s : woodNames) {
            Block wood = new Block(-1).setName(s+" log");
            ArrayList<String> list2 = Lists.newArrayList();
            list2.add("wood/"+s+"_0");
            list2.add("wood/"+s+"_1");
            wood.setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
            wood.setTextures(list2.toArray(new String[list2.size()]));
            addBlock(wood);
        }
        
    }
    @Override
    public List<String> getNames() {
        return woodNames;
    }
}