package nidefawl.qubes.logging;

import nidefawl.qubes.util.GameError;

public class ErrorHandler {
    static IErrorHandler handler;
    public static void setException(GameError gameError) {
        if (handler != null) {
            handler.setException(gameError);
        }
    }
    public static void setHandler(IErrorHandler iHandler) {
        handler = iHandler;
    }

}
