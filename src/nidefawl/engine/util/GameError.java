package nidefawl.engine.util;

public class GameError extends RuntimeException {

    public GameError(String string) {
        super(string);
    }

    public GameError(String string, Exception e) {
        super(string, e);
    }

}
