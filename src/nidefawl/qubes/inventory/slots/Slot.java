/**
 * 
 */
package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.ItemStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Slot {
    public BaseInventory inv;
    public int           idx;
    public float         x;
    public float         y;
    public float         w;
    protected Slots slots;

    public Slot(Slots slots, BaseInventory inv, int i, float x, float y, float w) {
        this.slots = slots;
        this.inv = inv;
        this.idx = i;
        this.x = x;
        this.y = y;
        this.w = w;
    }

    /**
     * @return
     */
    public BaseStack getItem() {
        return this.inv.getItem(this.idx);
    }

    /**
     * @param x2
     * @param y2
     * @return
     */
    public boolean isAt(double x, double y) {
        return x>=this.x&&x<=this.x+this.w&&y>=this.y&&y<=this.y+this.w;
    }

    public boolean transferTo(Slots out) {
        if (this.canTake()) {
            Slot output = out.getFirstEmpty(this.getItem());
            if (output != null && output.canPut(this.getItem())) {
                output.put(this);
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.getItem()==null;
    }

    public BaseStack drain() {
        return this.inv.setItem(this.idx, null);
    }

    public BaseStack put(Slot other) {
        return this.inv.setItem(this.idx, other.drain());
    }

    public BaseStack putStack(BaseStack stack) {
        if (this.canPut(stack)) {
            return this.inv.setItem(this.idx, stack);
        }
        return stack;
    }

    public boolean canTake() {
        return slots.canModify();
    }

    public boolean canPut(BaseStack stack) {
        return slots.canModify();
    }

}
