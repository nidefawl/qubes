/**
 * 
 */
package nidefawl.qubes.modules;

import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.Side;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public abstract class Module {

    public abstract void onLoad(Side side);
    public void onServerStarted(GameServer server) {};

    /**
     * @return
     */
    public String getModuleName() {
        return getClass().getSimpleName();
    }

}
