package nidefawl.qubes.commands;

import nidefawl.qubes.GameServer;

public interface ICommandSource {

    void preExecuteCommand(Command c);

    void onUnknownCommand(String cmd, String line);

    GameServer getServer();

    void onError(Command c, Exception e);

}
