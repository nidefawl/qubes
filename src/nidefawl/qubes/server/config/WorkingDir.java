package nidefawl.qubes.server.config;

import java.io.File;

public class WorkingDir {
	
	static File workingDir = new File(".");
	
	public static void init() {
		new File(workingDir, "config").mkdirs();
	}

}
