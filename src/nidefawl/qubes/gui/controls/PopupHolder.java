/**
 * 
 */
package nidefawl.qubes.gui.controls;

import nidefawl.qubes.gui.AbstractUI;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface PopupHolder {

    /**
     * @param object
     */
    void setPopup(AbstractUI object);

    /**
     * @return
     */
    AbstractUI getPopup();

}
