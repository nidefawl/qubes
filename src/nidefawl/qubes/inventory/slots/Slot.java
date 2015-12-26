/**
 * 
 */
package nidefawl.qubes.inventory.slots;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Slot {
    public BaseInventory inv;
    public int           idx;
    public float         x;
    public float         y;
    public float         w;

    public Slot(BaseInventory inv, int i, float x, float y, float w) {
        this.inv = inv;
        this.idx = i;
        this.x = x;
        this.y = y;
        this.w = w;
    }

    /**
     * @return
     */
    public BaseStack getItem() {
        return this.inv.getItem(this.idx);
    }

    /**
     * @param x2
     * @param y2
     * @return
     */
    public boolean isAt(double x, double y) {
        return x>=this.x&&x<=this.x+this.w&&y>=this.y&&y<=this.y+this.w;
    }

}
