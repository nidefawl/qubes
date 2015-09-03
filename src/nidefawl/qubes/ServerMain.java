package nidefawl.qubes;

import nidefawl.qubes.config.GameServer;
import nidefawl.qubes.config.WorkingEnv;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ServerMain {

	public static void main(String[] args) {
		try {
			WorkingEnv.init();
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
