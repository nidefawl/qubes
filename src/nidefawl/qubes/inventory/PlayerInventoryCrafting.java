/**
 * 
 */
package nidefawl.qubes.inventory;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerInventoryCrafting extends BaseInventory {
    
    private int inputSlots;
    private int outputSlots;
    public PlayerInventoryCrafting(int id, int inputSlots, int outputSlots) {
        super(id, inputSlots+outputSlots);
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
    }
    
    public int getInputSlots() {
        return this.inputSlots;
    }
    public int getOutputSlots() {
        return this.outputSlots;
    }

    public PlayerInventoryCrafting copy(PlayerInventoryCrafting inv) {
        for (int i = 0; i < this.stacks.length; i++) {
            if (this.stacks[i] != null)
                inv.stacks[i] = this.stacks[i].copy();
        }
        return inv;
    }
}
