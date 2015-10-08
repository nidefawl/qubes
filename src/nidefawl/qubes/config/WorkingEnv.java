package nidefawl.qubes.config;

import java.io.File;
import java.util.Locale;

import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;

public class WorkingEnv {

    static File workingDir = new File(".");
    static File assetDir;
    static File packsDir;
    static File worlds;
    static File config;
    static File playerdata;
    
	public static void init(String basePath) {
	    Side side = GameContext.getSide();
        Locale.setDefault(Locale.US);
        workingDir = new File(basePath);
        workingDir.mkdirs();
        config = new File(workingDir, "config");
        worlds = new File(workingDir, "worlds");
        playerdata = new File(workingDir, "playerdata");
        assetDir = new File(workingDir, "res");
        packsDir = new File(workingDir, "packs");
        if (side == Side.CLIENT) {
            assetDir.mkdirs();
        }
        if (side == Side.SERVER) {
            playerdata.mkdirs();
        }
		config.mkdirs();
		worlds.mkdirs();
	}

    public static File getAssetFolder() {
        return assetDir;
    }
    public static File getPacksFolder() {
        return packsDir;
    }
    public static File getConfigFolder() {
        return config;
    }
    public static File getWorldsFolder() {
        return worlds;
    }
    
    public static File getPlayerData() {
        return playerdata;
    }

    /**
     * @return
     */
    public static File getWorkingDir() {
        return workingDir;
    }

}
