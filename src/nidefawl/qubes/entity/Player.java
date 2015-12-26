package nidefawl.qubes.entity;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.item.BaseStack;

public abstract class Player extends Entity {

    public String        name;
    public int punchTicks;
    final PlayerInventory inventory = new PlayerInventory();
    final PlayerInventoryCrafting inventoryCraft = new PlayerInventoryCrafting();
    public Player() {
        super();
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }
    @Override
    public void tickUpdate() {
        super.tickUpdate();
        updateTicks();
    }
    public void updateTicks() {
        if (this.punchTicks > 0) {
            this.punchTicks--;
        }
    }

    /**
     * 
     */
    public BaseStack getEquippedItem() {
        return inventory.getItem(0);
    }


    public final PlayerInventory getInventory() {
        return inventory;
    }
    public final PlayerInventoryCrafting getCraftInventory() {
        return inventoryCraft;
    }
    public BaseInventory getInv(int id) {
        switch (id) {
            case 0:
                return getInventory();
            case 1:
                return getCraftInventory();
        }
        return null;
    }
    public Slots getSlots(int id) {
        return null;
    }
}
