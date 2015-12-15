package nidefawl.qubes.block;

import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupOres  extends BlockGroup {
    final List<String> stoneNames = Lists.newArrayList();
    public BlockGroupOres(BlockGroup b) {
        stoneNames.addAll(b.getNames());
        for (Block s : b.getBlocks()) {
            addBlock(new BlockOre(-1, s).setName(s.getName()+" ore"));
        }
    }

    @Override
    public List<String> getNames() {
        return this.stoneNames;
    }
}
