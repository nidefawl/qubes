package nidefawl.qubes.util;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.IDMappingBlocks;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.crafting.CraftingManager;
import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.EntityType;
import nidefawl.qubes.item.IDMappingItems;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.ItemModel;

public class GameContext {
    static Thread mainThread;
    static Side side;
    static GameError initError;
    private static long startBoot;
    public static long getTimeSinceStart() {
        return System.currentTimeMillis()-startBoot;
    }
    

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
        startBoot = System.currentTimeMillis();
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
            AssetManager.init();
            IDMappingBlocks.load();
            IDMappingItems.load();
            EntityType.load();
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
            EntityModel.preInit();
            Block.preInit();
            Item.preInit();
            Block.postInit();
            Item.postInit();
            ItemModel.postInit();
            EntityModel.postInit();
            IDMappingBlocks.save();
            IDMappingItems.save();
            CraftingRecipes.init();
            
        } catch (GameError e) {
            initError = e;
        } catch (Exception e) {
            initError = new GameError(e);
        }
    }
    

}
