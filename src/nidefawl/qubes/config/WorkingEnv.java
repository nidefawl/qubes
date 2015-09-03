package nidefawl.qubes.config;

import java.io.File;
import java.util.Locale;

public class WorkingEnv {

    static File workingDir = new File(".");
    static File assetDir;
    static File worlds;
    static File config;
	
	public static void init() {
        Locale.setDefault(Locale.US);
        config = new File(workingDir, "config");
        worlds = new File(workingDir, "worlds");
		if (new File("res").isDirectory()) {
		    assetDir = new File("res");
		} else {
		    assetDir = new File(workingDir, "res");
		    assetDir.mkdirs();
		}
		config.mkdirs();
		worlds.mkdirs();
	}

    public static File getAssetFolder() {
        return assetDir;
    }

    public static File getWorldsFolder() {
        return worlds;
    }

}
