package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.item.BaseStack;

public abstract class Slot {

    public int           idx;
    public float         x;
    public float         y;
    public float         w;
    protected Slots slots;

    public Slot(Slots slots, int i, float x, float y, float w) {
        this.slots = slots;
        this.idx = i;
        this.x = x;
        this.y = y;
        this.w = w;
    }

    public abstract BaseStack getItem();

    /**
     * @param x2
     * @param y2
     * @return
     */
    public boolean isAt(double x, double y) {
        return x>=this.x&&x<=this.x+this.w&&y>=this.y&&y<=this.y+this.w;
    }

    public abstract boolean transferTo(SlotsInventoryBase out);

    public abstract boolean isEmpty();

    public abstract BaseStack drain();

    public abstract BaseStack put(SlotInventory other);

    public abstract BaseStack putStack(BaseStack stack);

    public abstract boolean canTake();

    public abstract boolean canPut(BaseStack stack);

    public void flag() {
    }
}
