/**
 * 
 */
package nidefawl.qubes;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
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
