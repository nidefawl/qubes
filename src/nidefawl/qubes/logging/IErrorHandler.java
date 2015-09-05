package nidefawl.qubes.logging;

import nidefawl.qubes.util.GameError;

public interface IErrorHandler {

    void setException(GameError gameError);

}
