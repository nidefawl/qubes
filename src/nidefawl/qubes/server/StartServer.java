package nidefawl.qubes.server;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.noise.NoiseLib;
import nidefawl.qubes.noise.TerrainNoiseScale;
import nidefawl.qubes.util.*;
import nidefawl.qubes.worldgen.TerrainGen;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@SideOnly(value = Side.SERVER)
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
			GameContext.setSideAndPath(Side.SERVER, ".");
			GameContext.earlyInit();
			GameError err = GameContext.getInitError();
			if (err != null) {
			    throw err;
			}
	        GameContext.lateInit();
	        TerrainGen.init();
            err = GameContext.getInitError();
            if (err != null) {
                throw err;
            }
			final GameServer instance = new GameServer();
	        Runtime.getRuntime().addShutdownHook(new Thread(
	        	new Runnable() {
					public void run() {
		            	instance.stopServer();
					}
				}
	        ));
	        SignalHandler handler = new SignalHandler () {
	            public void handle(Signal sig) {
	            	instance.stopServer();
                }
            };
            nidefawl.qubes.server.ConsoleReader.startThread(instance);
	        Signal.handle(new Signal("INT"), handler);
	        ErrorHandler.setHandler(instance);
            instance.loadConfig();
			instance.startServer();
			GameContext.setMainThread(instance.getThread());
		} catch (Exception e) {
			System.err.println("Failed starting server");
			e.printStackTrace();
		}
	}
}
