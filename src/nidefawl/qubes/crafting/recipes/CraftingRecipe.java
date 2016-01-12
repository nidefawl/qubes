package nidefawl.qubes.crafting.recipes;

import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.inventory.slots.SlotInventory;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.inventory.slots.SlotsInventoryBase;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;

public class CraftingRecipe {
    static int NEXT_ID = 0;

    private BaseStack[] in;
    private BaseStack[] out;
    private int id;
    private BaseStack preview;
    CraftingCategory category;

    private String subCat;

    public CraftingRecipe() {
        this.id = NEXT_ID++;
        CraftingRecipes.add(this);
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
    public CraftingCategory getCategory() {
        return this.category;
    }
    public void setInput(BaseStack... itemStack) {
        for (int n = 0; n < itemStack.length; n++) {
            if (itemStack[n].getSize() <= 0) {
                throw new IllegalArgumentException("Invalid stack size for recipe input");
            }
        }
        this.in = itemStack;
    }
    public void setOutput(BaseStack... itemStack) {
        for (int n = 0; n < itemStack.length; n++) {
            if (itemStack[n].getSize() <= 0) {
                throw new IllegalArgumentException("Invalid stack size for recipe output");
            }
        }
        this.out = itemStack;
        if (this.preview == null) {
            this.preview = this.out[0];
        }
    }
    public void setCategory(CraftingCategory craftingCategory, String subCat) {
        this.category = craftingCategory;
        this.subCat = subCat;
    }

    public String getSubCat() {
        return this.subCat;
    }
}
