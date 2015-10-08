package nidefawl.qubes;

import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;

public class BootClient {
    public static void main(String[] args) {
        GameContext.earlyInit(Side.CLIENT, ".");
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
