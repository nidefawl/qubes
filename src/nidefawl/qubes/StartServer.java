package nidefawl.qubes;

import jline.console.ConsoleReader;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class StartServer {

	public static void main(String[] args) {
		try {
			WorkingEnv.init(Side.SERVER, ".");
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
            nidefawl.qubes.server.ConsoleReader.startThread(instance);
	        Signal.handle(new Signal("INT"), handler);
	        ErrorHandler.setHandler(instance);
			instance.startServer();
			GameContext.setMainThread(instance.getThread());
		} catch (Exception e) {
			System.err.println("Failed starting server");
			e.printStackTrace();
		}
	}
}
