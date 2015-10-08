/**
 * 
 */
package nidefawl.qubes.event;

import nidefawl.qubes.modules.Module;
import nidefawl.qubes.modules.ModuleLoader;
import nidefawl.qubes.server.GameServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Events {


    public static void onServerStarted(GameServer gameServer) {
        Module[] modules = ModuleLoader.getModulesArray();
        for (int i = 0; i < modules.length; i++) {
            modules[i].onServerStarted(gameServer);
        }
    }

}
