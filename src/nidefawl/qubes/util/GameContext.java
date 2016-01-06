package nidefawl.qubes.util;

import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.IDMapping;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.models.ItemModel;
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

    public static void setSideAndPath(Side s, String path) {
        side = s;
        WorkingEnv.init(path);
    }
    /**
     * @param server
     * @param string
     */
    public static void earlyInit() {
        try {
            AsyncTasks.init();
            IDMapping.load();
            ModuleLoader.scanModules(WorkingEnv.getModulesDir());
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
            ItemModel.preInit();
            Block.preInit();
            Item.preInit();
            ModuleLoader.loadModules();
            Block.postInit();
            Item.postInit();
            ItemModel.postInit();
            IDMapping.save();
            
        } catch (GameError e) {
            initError = e;
        } catch (Exception e) {
            initError = new GameError(e);
        }
    }
    

}
