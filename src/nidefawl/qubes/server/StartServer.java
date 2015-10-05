package nidefawl.qubes.server;

import java.io.File;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.noise.NoiseLib;
import nidefawl.qubes.noise.TerrainNoiseScale;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class StartServer {

	public static void main(String[] args) {
		try {
		    boolean b = NoiseLib.isLibPresent();
		    if (b) {
		        System.out.println("Using native library for OpenSimplexNoise");
	            double scaleMixXZ = 4.80D;
	            double scaleMixY = scaleMixXZ*0.4D;
	            TerrainNoiseScale noise = NoiseLib.newNoiseScale(4)
	                    .setUpsampleFactor(8)
	                    .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
	                    .setOctavesFreq(3, 2);
	            double[] data = noise.gen(0, 0);
		        System.out.println(data[0]+"/"+data[1]);
		    } else {
		        System.err.println("Native library for OpenSimplexNoise not found");
		    }
			WorkingEnv.init(Side.SERVER, ".");
            PluginLoader.loadPlugins(new File(WorkingEnv.getWorkingDir(), "plugins"));
            PluginLoader.registerModules();
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
