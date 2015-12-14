/**
 * 
 */
package nidefawl.qubes.inventory;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.item.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PlayerInventory extends BaseInventory {
    
    public final int inventorySize = 10*4;
    public final BaseStack[] stacks = new BaseStack[inventorySize];
    public BaseStack carried;
    
    public PlayerInventory() {
        stacks[0] = new ItemStack(Item.axe);
        stacks[1] = new ItemStack(Item.pickaxe);
        stacks[2] = new BlockStack(Block.dirt);
        stacks[3] = new BlockStack(Block.grass);
    }

    /**
     * @return
     */
    public PlayerInventory copy() {
        PlayerInventory inv = new PlayerInventory();
        for (int i = 0; i < this.stacks.length; i++) {
            inv.stacks[i] = this.stacks[i].copy();
        }
        return inv;
    }
    @Override
    public BaseStack getItem(int idx) {
        return this.stacks[idx];
    }

    /**
     * @param item
     */
    public BaseStack setCarried(BaseStack item) {
        BaseStack cur = this.carried;
        this.carried = item;
        return cur;
    }

    @Override
    public BaseStack getCarried() {
        return this.carried;
    }

    @Override
    public void setItem(int idx, BaseStack item) {
        this.stacks[idx] = item;
    }
}
