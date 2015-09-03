package nidefawl.qubes;

import nidefawl.qubes.config.WorkingEnv;

public class BootClient {

    static public Game instance;

    public static void main(String[] args) {
        try {
            WorkingEnv.init();
        } catch (Exception e) {
            System.err.println("Failed starting game");
            e.printStackTrace();
        }
        GameBase.appName = "-";
        instance = new Game();
        instance.startGame();
    }
}
