package nidefawl.qubes.item;

import java.util.List;

import nidefawl.qubes.block.BlockGroupLogs;

public class ItemGroupLog extends ItemGroup {

    public ItemGroupLog() {
        List<String> names = BlockGroupLogs.logNames;
        int size = names.size();
        for (int i = 0; i < size; i++) {
            String s = names.get(i);
            ItemLog wood = new ItemLog(s+"_log", i);
            wood.setTextures("log/"+s);
            addItem(wood);
        }
    }
    public java.util.List<String> getNames() {
        return BlockGroupLogs.logNames;
    }
}
