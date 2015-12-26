package nidefawl.qubes.inventory.slots;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.item.BaseStack;

public class Slots {
    protected BaseInventory baseInv;
    private PlayerInventory playerInv;
    protected List<Slot> slots = Lists.newArrayList();
    private int id;
    public Slots(int id, PlayerInventory playerInv, BaseInventory baseInv) {
        this.id = id;
        this.baseInv = baseInv;
        this.playerInv = playerInv;
    }
    public int getId() {
        return this.id;
    }

    protected void addSlot(Slot slot) {
        this.slots.add(slot);
    }
    /**
     * @param d
     * @param e
     * @return 
     */
    public Slot getSlotAt(double x, double y) {
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i).isAt(x, y)) {
                return this.slots.get(i);
            }
        }
        return null;
    }
    
    /**
     * @return the slots
     */
    public List<Slot> getSlots() {
        return this.slots;
    }
    
    /**
     * @return the inv
     */
    public BaseInventory getInv() {
        return this.baseInv;
    }
    /**
     * @param s
     * @param button
     * @param action
     * @return 
     */
    public BaseStack slotClicked(Slot s, int button, int action) {
        if (button == 0 && action == 0) {
            BaseStack stack = s.getItem();
            BaseStack prevcarried = playerInv.setCarried(stack);
            this.baseInv.setItem(s.idx, prevcarried);
            return stack;
        }
        return null;
    }

    public Slot getSlot(int idx) {
        return idx>=0&&idx<this.slots.size()?this.slots.get(idx):null;
    }

}
