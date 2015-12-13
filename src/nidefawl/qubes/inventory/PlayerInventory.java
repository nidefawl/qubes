/**
 * 
 */
package nidefawl.qubes.inventory;

import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerInventory extends BaseInventory {
    
    public final int inventorySize = 10*4;
    BaseStack[] stacks = new BaseStack[inventorySize];
    
    public PlayerInventory() {
    }
}
