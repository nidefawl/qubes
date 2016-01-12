package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.inventory.InventoryUtil;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.item.BaseStack;

public class SlotStock extends Slot {

    public BaseStack stack;
    public PlayerInventory inv;
    public BaseStack stackReq;

    public SlotStock(Slots slots, BaseStack stack, PlayerInventory inv, int i, float x, float y, float w) {
        super(slots, i, x, y, w);
        this.stackReq = stack.copy();
        this.stack = stack.copy();
        this.stack.setSize(0);
        this.inv = inv;
    }

    @Override
    public BaseStack getItem() {
        return stack;
    }

    @Override
    public boolean transferTo(SlotsInventoryBase out) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public BaseStack drain() {
        return null;
    }

    @Override
    public BaseStack put(SlotInventory other) {
        return null;
    }

    @Override
    public BaseStack putStack(BaseStack stack) {
        return null;
    }

    @Override
    public boolean canTake() {
        return false;
    }

    @Override
    public boolean canPut(BaseStack stack) {
        return false;
    }

    public void update() {
        if (this.inv == null)
            return;
        Integer i = this.inv.getSortedStacks().get(this.stack.getTypeHash());
        if (i == null)
            i=0;
        this.stack.setSize(i);
    }

}
