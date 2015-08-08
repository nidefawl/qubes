package nidefawl.qubes.util;

public class GameError extends RuntimeException {

    private static final long serialVersionUID = -4621355890152198087L;

    public GameError(String string) {
        super(string);
    }

    public GameError(String string, Exception e) {
        super(string, e);
    }

}
