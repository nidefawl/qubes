package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.item.BaseStack;

public class SlotPreview extends Slot {

    private BaseStack stack;

    public SlotPreview(Slots slots, BaseStack baseStack, int i, float x, float y, float w) {
        super(slots, i, x, y, w);
        this.stack = baseStack;
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

}
