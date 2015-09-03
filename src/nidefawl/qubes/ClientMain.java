package nidefawl.qubes;

public class ClientMain {

    static public Main instance;
    public static String[] lastargs;
    public static void main(String[] args) {
        lastargs = args;
        instance   = new Main();
        instance.startGame();
    }
}
