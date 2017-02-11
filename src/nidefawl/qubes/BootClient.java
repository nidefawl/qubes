package nidefawl.qubes;

import org.lwjgl.system.Configuration;

import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;

@SideOnly(value = Side.CLIENT)
public class BootClient {
    public static int appId = 0;
    

    public static void main(String[] args) {
        boolean debug = Boolean.valueOf(System.getProperty("game.debug", "false"));
        Configuration.DEBUG.set(debug);
        Configuration.DEBUG_MEMORY_ALLOCATOR.set(false);
        Configuration.DISABLE_CHECKS.set(true);
        Configuration.GLFW_CHECK_THREAD0.set(false);
        Configuration.MEMORY_ALLOCATOR.set("jemalloc");
//        Configuration.MEMORY_DEFAULT_ALIGNMENT.set("cache-line");
        System.setProperty("jna.debug_load.jna", "true");
        System.setProperty("jna.nounpack", "true");
        System.setProperty("jna.boot.library.path", ".");
        GameContext.setSideAndPath(Side.CLIENT, ".");
        GameContext.earlyInit();
        GameBase.appName = "-";
        GameBase baseInstance = getInstance();
        baseInstance.setException(GameContext.getInitError());
        baseInstance.parseCmdArgs(args);
        ErrorHandler.setHandler(GameBase.baseInstance);
        baseInstance.startGame();
        GameContext.setMainThread(GameBase.baseInstance.getMainThread());
        if (GameContext.getMainThread().isAlive()) {
            if (NativeInterface.isPresent()) {
                NativeInterface.getInstance().gameAlive();
            }
        }
        while(GameContext.getMainThread().isAlive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("OVER!");
    }

    private static GameBase getInstance() {
        String instanceClass = null;
        switch (appId) {
            case 1:
                instanceClass = "test.game.ParticlePerformanceTest";
                break;
            case 2:
                instanceClass = "test.game.vr.VRApp";
                break;
        }
        if (instanceClass != null) {
            try {
                Class<?> c = Class.forName(instanceClass);
                return (GameBase) c.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed creating app instance", e);            
            }
        }
        return new Game();
    }
}
