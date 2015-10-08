package nidefawl.qubes.util;

import java.io.File;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.modules.ModuleLoader;

public class GameContext {
    static Thread mainThread;
    static Side side;
    static GameError initError;

    public static void setMainThread(Thread thread) {
        mainThread = thread;
    }
    
    
    /**
     * Do not check against at boottime
     * @return
     */
    public static Thread getMainThread() {
        return mainThread;
    }
    
    /**
     * @return the side
     */
    public static Side getSide() {
        return side;
    }


    /**
     * 
     */
    public static void earlyInit() {
        // TODO Auto-generated method stub
        
    }


    /**
     * @param server
     * @param string
     */
    public static void earlyInit(Side s, String path) {
        side = s;
        try {

            WorkingEnv.init(path);
            ModuleLoader.scanModules(new File(WorkingEnv.getWorkingDir(), "modules"));
        } catch (GameError e) {
            initError = e;
        } catch (Exception e) {
            initError = new GameError(e);
        }
    }
    
    /**
     * @return the initError
     */
    public static GameError getInitError() {
        return initError;
    }


    /**
     */
    public static void lateInit() {
        initError = null;
        try {
            Block.preInit();
            ModuleLoader.loadModules();
            Block.postInit();
            
        } catch (GameError e) {
            initError = e;
        } catch (Exception e) {
            initError = new GameError(e);
        }
    }

}
