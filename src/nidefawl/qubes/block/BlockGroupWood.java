package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupWood extends BlockGroup {

    public BlockGroupWood() {
        int size = BlockGroupLogs.logNames.size();
        for (int i = 0; i < size; i++) {
            String s = BlockGroupLogs.logNames.get(i);
            Block wood = new BlockWood(s+"_wood", i);
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
        return BlockGroupLogs.logNames;
    }
}