/**
 * 
 */
package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SlotsInventory extends SlotsInventoryBase {


    public SlotsInventory(Player player, int xPos, int yPos, int w, int dist) {
        super(0, player.getInventory(), player.getInventory());
        for (int i = 0; i < this.baseInv.inventorySize; i++) {
            addSlot(new SlotInventory(this, this.baseInv, i, xPos+(i%10)*(w+dist), yPos+(i/10)*(w+dist), w));
        }
    }

    public SlotsInventory(PlayerServer player) {
        this(player, 0, 0, 32, 2);
    }

    public boolean canModify() {
        return true;
    }
    
}
