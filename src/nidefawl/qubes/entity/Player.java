package nidefawl.qubes.entity;

import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.inventory.slots.SlotsInventory;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.util.GameMath;

public abstract class Player extends Entity {

    public String        name;
    public int punchTicks;
    final PlayerInventory inventory = new PlayerInventory();
    protected SlotsInventory slotsInventory;
     final SlotsCrafting[] slotsCrafting = new SlotsCrafting[CraftingCategory.NUM_CATS];
    final PlayerInventoryCrafting[] inventoryCraft = new PlayerInventoryCrafting[CraftingCategory.NUM_CATS];
    public float armOffsetPitchPrev;
    public float armOffsetYawPrev;
    public float armOffsetPitch;
    public float armOffsetYaw;
    
    public Player() {
        super();
        for (int i = 0; i < CraftingCategory.NUM_CATS; i++) {
            inventoryCraft[i] = new PlayerInventoryCrafting(1+i, 4, 4);
        }
        this.properties = new EntityProperties();
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public void tickUpdate() {
        super.tickUpdate();
        updateTicks();
//        if (yaw-armOffsetYawPrev < -180) {
//            armOffsetYaw+=180;
//        }
//        if (yaw-armOffsetYawPrev > 180) {
//            armOffsetYaw-=180;
//        }
//        if (Math.abs(yaw-armOffsetYawPrev) > 90) {
//            System.out.println("err: "+(yaw-armOffsetYawPrev));
//        }
        armOffsetPitchPrev=armOffsetPitch;
        armOffsetYawPrev=armOffsetYaw;
        armOffsetYaw = armOffsetYaw + (yaw-armOffsetYaw)*0.38F;
        armOffsetPitch = armOffsetPitch + (pitch-armOffsetPitch)*0.38F;

//        armOffsetYaw=GameMath.wrapAngle(this.armOffsetYaw);
//        armOffsetYawPrev=GameMath.wrapAngle(this.armOffsetYawPrev);
    }
    public void updateTicks() {
        if (this.punchTicks > 0) {
            this.punchTicks--;
        }
    }


    public final PlayerInventory getInventory() {
        return inventory;
    }
    
    public BaseInventory getInv(int id) {
        switch (id) {
            case 0:
                return getInventory();
            case 1:
                return inventoryCraft[0];
            case 2:
                return inventoryCraft[1];
            case 3:
                return inventoryCraft[2];
            case 4:
                return inventoryCraft[3];
        }
        return null;
    }

    public Slots getSlots(int id) {
        switch (id) {
            case 0:
                return this.slotsInventory;
            case 1:
                return slotsCrafting[0];
            case 2:
                return slotsCrafting[1];
            case 3:
                return slotsCrafting[2];
            case 4:
                return slotsCrafting[3];
        }
        return null;
    }

    public EntityModel getEntityModel() {
        EntityProperties properties = this.properties;
        if (properties != null) {
            int n = properties.getOption(15, 0);
            if (n != 0) {
                return EntityModel.modelPlayerFemale;
            }
        }
        return EntityModel.modelPlayerMale;
    }

}
