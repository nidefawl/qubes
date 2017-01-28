package nidefawl.qubes.util;

public class GameLogicError extends RuntimeException {

    private static final long serialVersionUID = -4621355890152198087L;

    public GameLogicError(Throwable t) {
        super(t);
    }
    public GameLogicError(Exception t) {
        super(t);
    }
    public GameLogicError(String string) {
        super(string);
    }

    public GameLogicError(String string, Throwable t) {
        super(string, t);
    }

    public GameLogicError(String string, Exception t) {
        super(string, t);
    }

}
