/**
 * 
 */
package nidefawl.qubes.item;

import nidefawl.qubes.network.StreamIO;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class BaseStack implements StreamIO {

    public abstract boolean isItem();
    public boolean isBlock() {
        return !isItem();
    }
}
