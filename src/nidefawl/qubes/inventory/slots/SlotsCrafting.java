package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;

public class SlotsCrafting extends Slots {

    private PlayerInventoryCrafting inv;
    private Slot result;
    private boolean locked = false;
    public SlotsCrafting(Player player) {
        this(player, 0, 0, 32, 2);
    }

    public SlotsCrafting(Player player, int xPos, int yPos, int w, int dist) {
        super(1, player.getInventory(), player.getCraftInventory());
        this.inv = player.getCraftInventory();
        for (int i = 0; i < this.inv.inventorySize-1; i++) {
            addSlot(new Slot(this, this.inv, i, xPos+(i%8)*(w+dist), yPos+(i/8)*(w+dist), w));
        }
        addSlot(this.result=new Slot(this, this.inv, this.inv.inventorySize-1, xPos+(8)*(w+dist)+10, yPos+12, (int)(w*1.5)));
    }

    public Slot getResult() {
        return this.result;
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

}
