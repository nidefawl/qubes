package nidefawl.qubes.entity;

import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.item.BaseStack;

public abstract class Player extends Entity {

    public String        name;
    public int punchTicks;
    PlayerInventory inventory = new PlayerInventory();
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

}
