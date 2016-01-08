package nidefawl.qubes.item;

import java.util.List;

import com.google.common.collect.Lists;


public abstract class ItemGroup {

    static int NEXT_GROUP_ID = 0;
    final int id = NEXT_GROUP_ID++;
    public int getId() {
        return this.id;
    }

    private List<Item> items = Lists.newArrayList();

    void addItem(Item Item) {
        this.items.add(Item);
        Item.setItemGroup(this);
    }

    public abstract List<String> getNames();
    
    public List<Item> getItems() {
        return this.items;
    }
    public Item getItem(int index) {
        return index >= 0 && index <= this.items.size() ? this.items.get(index) : null;
    }
}
