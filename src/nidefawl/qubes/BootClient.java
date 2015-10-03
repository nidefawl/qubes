package nidefawl.qubes;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;

public class BootClient {
    public static void main(String[] args) {
        try {
            WorkingEnv.init(Side.CLIENT, ".");
        } catch (Exception e) {
            System.err.println("Failed starting game");
            e.printStackTrace();
        }

        //        String name = "me" + (GameMath.randomI(System.currentTimeMillis()));
        //        for (int i = 0; i < args.length; i++) {
        //            if (args[i].startsWith("-") && args[i].length() > 1) {
        //                if (i + 1 < args.length) {
        //                    if (args[i].substring(1).equalsIgnoreCase("name")) {
        //                        name = args[i + 1];
        //                    }
//                        }
        //            }
        //        }
        GameBase.appName = "-";
        Game.instance = new Game();
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
