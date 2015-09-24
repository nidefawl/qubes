/**
 * 
 */
package nidefawl.qubes.gui.controls;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface PopupHolder {

    /**
     * @param object
     */
    void setPopup(AbstractUIOverlay object);

    /**
     * @return
     */
    AbstractUIOverlay getPopup();

}
