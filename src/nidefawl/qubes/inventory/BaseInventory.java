/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public abstract class BaseInventory {

    public final int         id;
    public final int         inventorySize;
    public final BaseStack[] stacks;
    private byte[] flagged;
    boolean dirty = true;
    boolean needSorting = false;
    HashMap<Integer, Integer> amounts = Maps.newHashMap();

    public BaseInventory(int id, int inventorySize) {
        this.id = id;
        this.inventorySize = inventorySize;
        this.stacks = new BaseStack[inventorySize];
        this.flagged = new byte[inventorySize];
        this.needSorting = true;
        Arrays.fill(this.flagged, (byte)1);
    }
    public boolean isDirty() {
        return this.dirty;
    }

    public BaseStack getItem(int idx) {
        return this.stacks[idx];
    }

    public BaseStack setItem(int idx, BaseStack item) {
        BaseStack tmp = this.stacks[idx];
        this.stacks[idx] = item;
        this.flagged[idx] |= 1;
        this.dirty = true;
        this.needSorting = true;
        return tmp;
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

    public void addStack(BaseStack stack) {
        for (int i = 0; i < this.stacks.length; i++) {
            if (this.stacks[i] == null) {
                this.stacks[i] = stack;
                return;
            }
        }
    }

    public void setIncr(Collection<SlotStack> stacks) {
        _set(stacks, false);
    }
    public void set(Collection<SlotStack> stacks) {
        _set(stacks, true);
    }
    
    public void _set(Collection<SlotStack> stacks, boolean clear) {
        if (clear) {
            for (int i = 0; i < this.stacks.length; i++) {
                this.stacks[i] = null;
            }   
        }
        Iterator<SlotStack> it = stacks.iterator();
        while (it.hasNext()) {
            SlotStack slotStack = it.next();
            if (slotStack.slot >= 0 && slotStack.slot < this.stacks.length) {
                this.stacks[slotStack.slot] = slotStack.stack;
                it.remove();
            }
        }
        this.needSorting = true;
    }

    public HashSet<SlotStack> getUpdate() {
        this.dirty = false;
        HashSet<SlotStack> stacks = null;
        for (int i = 0; i < this.stacks.length; i++) {
            if ((this.flagged[i]&0x1)!=0) {
                BaseStack stack = this.stacks[i];
                if (stacks == null) stacks = Sets.newHashSet();
                stacks.add(new SlotStack(i, stack==null?null:stack.copy()));
                this.flagged[i]&=~0x1;
            }
        }
        return stacks;
    }
    public void flag(int idx) {
        this.flagged[idx] |= 1;
        this.dirty = true;
    }
    public HashMap<Integer, Integer> getSortedStacks() {
        if (this.needSorting) {
            this.needSorting = false;
            HashMap<Integer, Integer> map = Maps.newHashMap();
            for (int i = 0; i < this.stacks.length; i++) {
                if (this.stacks[i] != null) {
                    int h = this.stacks[i].getTypeHash();
                    Integer n = map.get(h);
                    if (n == null)
                        n = 0;
                    n += this.stacks[i].getSize();
                    map.put(h, n);
                }
            }
            amounts = map;
        }
        return amounts;
    }
}
