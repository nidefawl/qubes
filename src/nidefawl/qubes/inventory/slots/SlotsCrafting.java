package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;

public class SlotsCrafting extends Slots {

    private PlayerInventoryCrafting inv;
    public SlotsCrafting(Player player) {
        this(player, 0, 0, 32, 2);
    }

    public SlotsCrafting(Player player, int xPos, int yPos, int w, int dist) {
        super(1, player.getInventory(), player.getCraftInventory());
        this.inv = player.getCraftInventory();
        for (int i = 0; i < this.inv.inventorySize; i++) {
            addSlot(new Slot(this.inv, i, xPos+(i%8)*(w+dist), yPos+(i/8)*(w+dist), w));
        }
    }

}
