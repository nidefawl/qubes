package nidefawl.qubes.crafting.recipes;

import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;

public class CraftingRecipe {

    private BaseStack[] in;
    private BaseStack[] out;
    private int id;
    private BaseStack preview;

    public CraftingRecipe(int id, BaseStack in, BaseStack out) {
        this.id = id;
        this.in = new BaseStack[] {in};
        this.out = new BaseStack[] {out};
        this.preview = out;
    }
    public int getId() {
        return this.id;
    }
    public BaseStack[] getIn() {
        return this.in;
    }
    public BaseStack[] getOut() {
        return this.out;
    }
    public BaseStack getPreview() {
        return this.preview;
    }
    
    public boolean matches(SlotsCrafting slots) {
        boolean match = false;
//        for (int i = 0; i < slots.getInputSize(); i++) {
//            Slot s = slots.getSlot(i);
//            BaseStack stack = s.getItem();
//            if (stack != null) {
//                if (!this.in.is(stack.getItem())) {
//                    return false;
//                }
//                match = true;
//            }
//        }
        return match;
    }
    public boolean isInput(Item item) {
//        return item != null && this.in.is(item);
        return false;
    }
    public long getTime() {
        return 5000;
    }

}
