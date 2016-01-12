package nidefawl.qubes.inventory.slots;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.item.BaseStack;

public abstract class Slots {
    protected List<Slot> slots = Lists.newArrayList();
    int id;

    public Slots(int id) {
        this.id = id;
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
     * @param s
     * @param button
     * @param action
     * @return 
     */
    public abstract BaseStack slotClicked(Slot s, int button, int action);

    public Slot getSlot(int idx) {
        return idx>=0&&idx<this.slots.size()?this.slots.get(idx):null;
    }

    public boolean canModify() {
        return true;
    }

}