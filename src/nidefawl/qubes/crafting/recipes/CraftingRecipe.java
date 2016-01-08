package nidefawl.qubes.crafting.recipes;

import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;

public class CraftingRecipe {

    private Item in;
    private Item out;
    private int id;

    public CraftingRecipe(int id, Item in, Item out) {
        this.id = id;
        this.in = in;
        this.out = out;
    }
    public int getId() {
        return this.id;
    }
    public Item getIn() {
        return this.in;
    }
    public Item getOut() {
        return this.out;
    }
    
    public boolean matches(SlotsCrafting slots) {
        boolean match = false;
        for (int i = 0; i < slots.getInputSize(); i++) {
            Slot s = slots.getSlot(i);
            BaseStack stack = s.getItem();
            if (stack != null) {
                if (stack.getItem() != this.in) {
                    return false;
                }
                match = true;
            }
        }
        return match;
    }
    public boolean isInput(Item item) {
        return item != null && item == this.in;
    }
    public long getTime() {
        return 5000;
    }

}
