package nidefawl.qubes.block;

import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupOres  extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupOres(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        for (Block s : b.getBlocks()) {
            addBlock(new BlockOre(s.getName()+"_ore", s));
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
