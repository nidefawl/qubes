package nidefawl.qubes.server.commands;

public class CommandException extends RuntimeException {

    public CommandException(String string, Exception e) {
        super(string, e);
    }

    public CommandException(String string) {
        super(string);
    }

}
