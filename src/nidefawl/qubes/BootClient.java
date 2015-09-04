package nidefawl.qubes;

import nidefawl.qubes.config.WorkingEnv;

public class BootClient {

    public static void main(String[] args) {
        try {
            WorkingEnv.init();
        } catch (Exception e) {
            System.err.println("Failed starting game");
            e.printStackTrace();
        }
        GameBase.appName = "-";
        Game.instance = new Game();
        Game.instance.startGame();
    }
}
