/**
 * 
 */
package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.PlayerInventory;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SlotsInventory extends Slots {

    private PlayerInventory inv;

    public SlotsInventory(Player player, int xPos, int yPos, int w, int dist) {
        super(0, player.getInventory(), player.getInventory());
        this.inv = player.getInventory();
        for (int i = 0; i < this.inv.inventorySize; i++) {
            addSlot(new Slot(this.inv, i, xPos+(i%10)*(w+dist), yPos+(i/10)*(w+dist), w));
        }
    }

    public SlotsInventory(PlayerServer player) {
        this(player, 0, 0, 32, 2);
    }
    
}
