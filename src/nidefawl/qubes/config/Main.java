package nidefawl.qubes.config;

import nidefawl.qubes.server.config.WorkingDir;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Main {

	public static void main(String[] args) {
		try {

			WorkingDir.init();
			final GameServer instance = new GameServer();
	        Runtime.getRuntime().addShutdownHook(new Thread(
	        	new Runnable() {
					public void run() {
		            	instance.halt();
					}
				}
	        ));
	        SignalHandler handler = new SignalHandler () {
	            public void handle(Signal sig) {
	            	instance.halt();
	            }
	        };
	        Signal.handle(new Signal("INT"), handler);
	        instance.loadConfig();
			instance.startServer();
		} catch (Exception e) {
			System.err.println("Failed starting server");
			e.printStackTrace();
		}
	}
}
