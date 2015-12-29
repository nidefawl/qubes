/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.Iterator;
import java.util.List;

import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerInventory extends BaseInventory {
    
    public BaseStack carried;
    
    public PlayerInventory() {
        super(0, 10*4);
    }

    public PlayerInventory copy(PlayerInventory inv) {
        for (int i = 0; i < this.stacks.length; i++) {
            if (this.stacks[i] != null)
                inv.stacks[i] = this.stacks[i].copy();
        }
        return inv;
    }

    public List<SlotStack> copySlotStacks() {
        List<SlotStack> list = super.copySlotStacks();
        BaseStack carried = this.carried;
        if (carried != null ){
            list.add(new SlotStack(255, carried.copy()));
        }
        return list;
    }
    public void set(List<SlotStack> list) {
        super.set(list);
        Iterator<SlotStack> it = list.iterator();
        while (it.hasNext()) {
            SlotStack slotStack = it.next();
            if (slotStack.slot == 255) {
                this.carried = slotStack.stack;
            } else {
                System.out.println("stack wasn't consumed "+slotStack.slot+" - "+slotStack.stack);
                add(slotStack.stack);
            }
        }
    }
    /**
     * @param item
     */
    public BaseStack setCarried(BaseStack item) {
        BaseStack cur = this.carried;
        this.carried = item;
        return cur;
    }

    public BaseStack getCarried() {
        return this.carried;
    }
}
