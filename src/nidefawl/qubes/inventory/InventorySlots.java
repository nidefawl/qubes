/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class InventorySlots {

    private PlayerInventory inv;
    private List<Slot> slots = Lists.newArrayList();

    /**
     * @param inventory
     * @param slotBDist 
     * @param w 
     * @param k 
     * @param xPos 
     */
    public InventorySlots(PlayerInventory inventory, int xPos, int yPos, int w, int dist) {
        this.inv = inventory;
        for (int i = 0; i < this.inv.inventorySize; i++) {
            this.slots.add(new Slot(this.inv, i, xPos+(i%10)*(w+dist), yPos+(i/10)*(w+dist), w));
        }
    }
    /**
     * @param d
     * @param e
     * @return 
     */
    public Slot getSlotAt(double x, double y) {
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i).isAt(x, y)) {
                return this.slots.get(i);
            }
        }
        return null;
    }
    
    /**
     * @return the slots
     */
    public List<Slot> getSlots() {
        return this.slots;
    }
    
    /**
     * @return the inv
     */
    public PlayerInventory getInv() {
        return this.inv;
    }
    /**
     * @param s
     * @param button
     * @param action
     */
    public void slotClicked(Slot s, int button, int action) {
        if (button == 0 && action == 0) {
            s.clicked();
        }
    }
    /**
     * @return
     */
    public BaseStack getCarried() {
        return this.inv.carried;
    }


}
