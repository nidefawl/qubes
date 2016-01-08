package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class BlockGroupLogs extends BlockGroup {
    public final static List<String> logNames = Lists.newArrayList(new String[] { 
            "acacia", "birch", "cocoa", "coconut", "ebony", "mahagoni", "oak", "pine", "redwood", "spruce", "walnut", "willow"
    });

    final public Block oak;
    public BlockGroupLogs() {
        ArrayList<String> list = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            list.add("destroy/destroy_stage_"+i);
        }
        int size = logNames.size();
        for (int i = 0; i < size; i++) {
            String s = logNames.get(i);
            BlockLog log = (BlockLog) new BlockLog(s+"_log", i);
            ArrayList<String> list2 = Lists.newArrayList();
            list2.add("logs/"+s);
            list2.add("logs/"+s+"_top");
            list2.addAll(list);
            log.setTextures(list2.toArray(new String[list2.size()]));
            addBlock(log);
        }
        oak = getBlocks().get(6);
    }
    
    @Override
    public List<String> getNames() {
        return logNames;
    }
}
