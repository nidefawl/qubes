package nidefawl.qubes.inventory.slots;


import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.util.GameError;

public class SlotsInventoryBase extends Slots {
    protected BaseInventory baseInv;
    PlayerInventory playerInv;
    public SlotsInventoryBase(int id, PlayerInventory playerInv, BaseInventory baseInv) {
        super(id);
        this.baseInv = baseInv;
        this.playerInv = playerInv;
    }
    public BaseStack slotClicked(Slot s, int button, int action) {
        if (button == 0 && action == 0) {
            if (s.canTake()) {
//                System.out.println("take from "+s.idx+" on inv "+this.getClass()+":"+this.getId());
//                System.out.println("item is "+s.getItem());
//                BaseInventory inv = ((SlotsInventoryBase)s.slots).getInv();
//                for (int i = 0; i < inv.inventorySize; i++) {
//                    System.out.println("inv["+inv.getId()+"].item["+i+"] = "+inv.stacks[i]);    
//                }
                if (s.canPut(playerInv.getCarried())) {
                    BaseStack newCarried = playerInv.setCarried(s.putStack(playerInv.getCarried()));
                    return newCarried;   
                }
            }
        }
        return null;
    }

    /**
     * @return the inv
     */
    public BaseInventory getInv() {
        return this.baseInv;
    }

    public Slot getFirstEmpty(BaseStack item) {
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i).isEmpty()) {
                if (this.slots.get(i).canPut(item)) {
                    return this.slots.get(i);
                }
            }
        }
        return null;
    }

    public BaseStack addStack(BaseStack stack) {
        Slot output = this.getFirstEmpty(stack);
        if (output != null) {
            BaseStack left = output.putStack(stack);
            if (left != null) {
                // should not happen!!!
                throw new GameError("Result slot was expected to be empty");
            }
            return null;
        }
        return stack;
    }
}
