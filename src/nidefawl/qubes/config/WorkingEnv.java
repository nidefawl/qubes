package nidefawl.qubes.config;

import java.io.File;
import java.util.Locale;

public class WorkingEnv {
	
	static File workingDir = new File(".");
	
	public static void init() {
        Locale.setDefault(Locale.US);
		new File(workingDir, "config").mkdirs();
	}

}
