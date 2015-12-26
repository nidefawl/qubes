/**
 * 
 */
package nidefawl.qubes.inventory;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerInventoryCrafting extends BaseInventory {
    
    public PlayerInventoryCrafting() {
        super(1, 8*2);
    }

    public PlayerInventoryCrafting copy(PlayerInventoryCrafting inv) {
        for (int i = 0; i < this.stacks.length; i++) {
            if (this.stacks[i] != null)
                inv.stacks[i] = this.stacks[i].copy();
        }
        return inv;
    }
}
