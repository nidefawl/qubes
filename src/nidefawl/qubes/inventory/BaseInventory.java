/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public abstract class BaseInventory {

    public final int         id;
    public final int         inventorySize;
    public final BaseStack[] stacks;

    public BaseInventory(int id, int inventorySize) {
        this.id = id;
        this.inventorySize = inventorySize;
        this.stacks = new BaseStack[inventorySize];
    }

    public BaseStack getItem(int idx) {
        return this.stacks[idx];
    }

    public void setItem(int idx, BaseStack item) {
        this.stacks[idx] = item;
    }

    public int getId() {
        return this.id;
    }

    public int getSize() {
        return stacks.length;
    }

    public List<SlotStack> copySlotStacks() {
        List<SlotStack> list = Lists.newArrayList();
        for (int i = 0; i < stacks.length; i++) {
            BaseStack stack = stacks[i];
            if (stack != null) {
                list.add(new SlotStack(i, stack.copy()));
            }
        }
        return list;
    }

    public void add(BaseStack stack) {
        for (int i = 0; i < this.stacks.length; i++) {
            if (this.stacks[i] == null) {
                this.stacks[i] = stack;
                return;
            }
        }
    }

    public void set(List<SlotStack> list) {
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = null;
        }
        Iterator<SlotStack> it = list.iterator();
        while (it.hasNext()) {
            SlotStack slotStack = it.next();
            if (slotStack.slot >= 0 && slotStack.slot < this.stacks.length) {
                this.stacks[slotStack.slot] = slotStack.stack;
                it.remove();
            }
        }
    }
}
