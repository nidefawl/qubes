/*
 * 
 */
package nidefawl.qubes.server.commands;

import nidefawl.qubes.server.GameServer;

public interface ICommandSource {

    /**
     * Fired before the executen of command c. May be used to prepared things post command resolve and pre permission check.
     *
     * @param c the c
     */
    void preExecuteCommand(Command c);

    /**
     * Fired when command couldn't be resolved.
     *
     * @param cmd the cmd
     * @param line the line
     */
    void onUnknownCommand(String cmd, String line);

    /**
     * Gets the commandssources current server.
     *
     * @return the server
     */
    GameServer getServer();

    /**
     * Fired when an exception was thrown.
     *
     * @param c the c
     * @param e the e
     */
    void onError(Command c, CommandException e);

    /**
     * Sends a message to the command source.
     *
     * @param format the format
     */
    void sendMessage(String format);

}
