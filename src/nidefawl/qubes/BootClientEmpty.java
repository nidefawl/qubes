/**
 * 
 */
package nidefawl.qubes;

import nidefawl.qubes.util.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
@NoDeploy
public class BootClientEmpty {

    public static void main(String[] args) {
        try {
            NativeInterface.getInstance().gameAlive();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
