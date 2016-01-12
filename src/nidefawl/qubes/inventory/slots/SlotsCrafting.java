package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;
import nidefawl.qubes.item.BaseStack;

public class SlotsCrafting extends SlotsInventoryBase {

    private boolean locked = false;
    public SlotsCrafting(Player player, int id) {
        this(player, id, 0, 0, 32, 2);
    }

    public SlotsCrafting(Player player, int id, int xPos, int yPos, int w, int dist) {
        super(id, player.getInventory(), player.getInv(id));
        for (int i = 0; i < this.baseInv.inventorySize; i++) {
            addSlot(new SlotInventory(this, this.baseInv, i, xPos+(i%4)*(w+dist), yPos+(i/4)*(w+dist), w));
        }
    }

    public int getSize() {
        return this.slots.size();
    }

    public int getInputSize() {
        return this.slots.size()-1;
    }

    public void unlock() {
        locked = false;
    }

    public void lock() {
        locked = true;
    }

    public boolean canModify() {
        return !locked;
    }

    public int getNumItems() {
        int j = 0;
        for (int i = 0; i < this.getInputSize(); i++) {
            if (!this.getSlot(i).isEmpty()) {
                j++;
            }
        }
        return j;
    }
    public int transferSlots(SlotsInventoryBase baseInv) {
        for (int i = 0; i < this.getInputSize(); i++) {
            Slot s = this.getSlot(i);
            BaseStack stack = s.getItem();
            if (stack != null) {
                if (!s.transferTo(baseInv)) {
                    return 1;   
                }
            }
        }
        return 0;
    }

}
