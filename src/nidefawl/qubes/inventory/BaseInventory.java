/**
 * 
 */
package nidefawl.qubes.inventory;

import nidefawl.qubes.item.BaseStack;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class BaseInventory {

    public abstract BaseStack getItem(int idx);
    public abstract BaseStack setCarried(BaseStack item);
    public abstract BaseStack getCarried();
    public abstract void setItem(int idx, BaseStack item);
}
