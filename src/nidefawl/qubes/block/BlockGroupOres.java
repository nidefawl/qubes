package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupOres  extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupOres(BlockGroup blockgroup, Block ...blocks) {
        stoneNames.addAll(blockgroup.getNames());
        ArrayList<String> list = Lists.newArrayList();
        for (Block s : blockgroup.getBlocks()) {
            if (s.getName().contains("obsidian"))
                continue;
            ArrayList<String> list1 = Lists.newArrayList();
            list1.add("ores/ore_"+s.getName());
            list1.addAll(list);
            addBlock(new BlockOre(s.getName()+"_ore", s, list1));
        }
        for (Block s : blocks) {
            ArrayList<String> list1 = Lists.newArrayList();
            list1.add("ores/ore_"+s.getName());
            list1.addAll(list);
            addBlock(new BlockOre(s.getName()+"_ore", s, list1));
            stoneNames.add(s.getName());
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
