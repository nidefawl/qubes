package nidefawl.qubes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.modules.ModuleLoader;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;

public class BootClient {
    static String getValue(String[] args, int i, String arg) {
        String val = i+1<args.length ? args[i+1] : null;
        int idx = args[i].indexOf("=");
        if (idx > 0 && args[i].length()>idx+1) {
            val = args[i].substring(idx+1);
        }
        if (val != null) {
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length()-1);
            }
            if (val.startsWith("'") && val.endsWith("'")) {
                val = val.substring(1, val.length()-1);
            }
        }
        if (val == null || val.isEmpty()) {
            throw new RuntimeException("Please provide arg for --"+arg);
        }
        return val;
    }
    public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, ".");
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && args[i].length()>1) {
                String arg = args[i].substring(1);
                if (arg.startsWith("-") && arg.length()>1) {
                    arg = arg.substring(1);
                }
                switch (arg) {
                    case "modules": {
                        String path = getValue(args, i, arg);
                        ModuleLoader.setOverrideModules(path);
                        break;
                    }
                    case "moduledir": {
                        try {
                            String path = getValue(args, i, arg);
                            File fpath = new File(path);
                            System.err.println("adding module direcotry "+fpath);
                            ModuleLoader.addURLs(fpath.toURI().toURL());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                        
                }
            }
        }
        GameContext.earlyInit();
        GameBase.appName = "-";
        Game.instance = new Game();
        Game.instance.setException(GameContext.getInitError());
        Game.instance.getProfile().setName("Player");
        ErrorHandler.setHandler(Game.instance);
        Game.instance.startGame();
        GameContext.setMainThread(Game.instance.getMainThread());
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
}
