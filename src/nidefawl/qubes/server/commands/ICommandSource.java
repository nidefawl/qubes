package nidefawl.qubes.server.commands;

import nidefawl.qubes.server.GameServer;

public interface ICommandSource {

    void preExecuteCommand(Command c);

    void onUnknownCommand(String cmd, String line);

    GameServer getServer();

    void onError(Command c, CommandException e);

    void sendMessage(String format);

}
