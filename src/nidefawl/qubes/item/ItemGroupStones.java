package nidefawl.qubes.item;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.block.BlockGroupLogs;

public class ItemGroupStones extends ItemGroup {

    final static List<String> names = Arrays.asList(new String[] {"granite", "basalt"});
    public ItemGroupStones() {
        int size = names.size();
        for (int i = 0; i < size; i++) {
            String s = names.get(i);
            ItemStone wood = new ItemStone(s, i);
            wood.setTextures("stone/"+s);
            addItem(wood);
        }
    }
    public java.util.List<String> getNames() {
        return names;
    };
}
