package nidefawl.qubes.config;

import java.io.File;
import java.util.Locale;

public class WorkingEnv {

    static File workingDir = new File(".");
    static File assetDir = new File(".");
	
	public static void init() {
        Locale.setDefault(Locale.US);
		new File(workingDir, "config").mkdirs();
		if (new File("res").isDirectory()) {
		    assetDir = new File("res");
		} else {
		    assetDir = new File(workingDir, "res");
		    assetDir.mkdirs();
		}
	}

    public static File getAssetFolder() {
        return assetDir;
    }

}
