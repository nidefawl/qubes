package nidefawl.qubes;

import nidefawl.game.GLGame;
import nidefawl.qubes.config.WorkingEnv;

public class Main {

    static public Client instance;

    public static void main(String[] args) {
        try {
            WorkingEnv.init();
        } catch (Exception e) {
            System.err.println("Failed starting game");
            e.printStackTrace();
        }
        GLGame.appName = "-";
        instance = new Client();
        instance.startGame();
    }
}
