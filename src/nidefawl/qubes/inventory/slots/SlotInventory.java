/**
 * 
 */
package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SlotInventory extends Slot {
    public BaseInventory inv;

    public SlotInventory(Slots slots, BaseInventory inv, int i, float x, float y, float w) {
        super(slots, i, x, y, w);
        this.inv = inv;
    }

    @Override
    public BaseStack getItem() {
        return this.inv.getItem(this.idx);
    }
    
    @Override
    public boolean transferTo(SlotsInventoryBase out) {
        if (this.canTake()) {
            Slot output = out.getFirstEmpty(this.getItem());
            if (output != null && output.canPut(this.getItem())) {
                output.put(this);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return this.getItem()==null;
    }

    @Override
    public BaseStack drain() {
        return this.inv.setItem(this.idx, null);
    }

    @Override
    public BaseStack put(SlotInventory other) {
        return this.inv.setItem(this.idx, other.drain());
    }

    @Override
    public BaseStack putStack(BaseStack stack) {
        if (this.canPut(stack)) {
            return this.inv.setItem(this.idx, stack);
        }
        return stack;
    }

    @Override
    public boolean canTake() {
        return slots.canModify();
    }

    @Override
    public boolean canPut(BaseStack stack) {
        return slots.canModify();
    }

    public void flag() {
        this.inv.flag(this.idx);
    }
}
