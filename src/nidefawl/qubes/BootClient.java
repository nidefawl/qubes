package nidefawl.qubes;

import java.io.*;

import org.lwjgl.system.Configuration;

import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.*;

@SideOnly(value = Side.CLIENT)
public class BootClient {
    public static int appId = 0;
    

    public static void main(String[] args) {
        boolean debug = Boolean.valueOf(System.getProperty("game.debug", "false"));
        boolean logFile = Boolean.valueOf(System.getProperty("game.uselogfile", "false"));
        FileOutputStream logFileStream=null;
        LogToFileStream outStream=null;
        LogToFileStream errStream=null;
        if (logFile) {

            try {
                File currentLogFile = new File("game.log");
                if (!currentLogFile.exists() || currentLogFile.canWrite()) {
                    logFileStream = new FileOutputStream(currentLogFile);
                    outStream = new LogToFileStream(System.out, logFileStream);
                    errStream = new LogToFileStream(System.err, logFileStream);
                    System.out.println("Logging to "+currentLogFile);
                    System.out.flush();
                    System.setOut(outStream);
                    System.setErr(errStream);
                } else {
                    System.out.println("Cannot write to logfile "+currentLogFile);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        String strappId = System.getProperty("game.appid", null);
        if (strappId != null) {
            appId = StringUtil.parseInt(strappId, appId);
        }
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

        try {
            if (logFileStream != null) {
                System.out.flush();
                System.err.flush();
                System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                if (outStream != null) {
                    outStream.close();
                }
                if (errStream != null) {
                    errStream.close();
                }
                logFileStream.write("---- End of craftland.log ----\n".getBytes());
                logFileStream.flush();
                logFileStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static GameBase getInstance() {
        try {
            Thread.sleep(2222);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        String instanceClass = null;
        switch (appId) {
            case 1:
                instanceClass = "test.game.ParticlePerformanceTest";
                break;
            case 2:
                instanceClass = "test.game.vr.VRApp";
                break;
            case 3:
                instanceClass = "test.game.streamingtest.TexturedMesh_VK";
                break;
            case 4:
                instanceClass = "test.game.streamingtest.TexturedMesh_GL";
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
