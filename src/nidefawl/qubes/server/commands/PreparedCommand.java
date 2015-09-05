package nidefawl.qubes.server.commands;

public class PreparedCommand {
    ICommandSource source;
    Command        c;
    String         cmd;
    String[]       args;
    String         line;

    public PreparedCommand(ICommandSource source, Command c, String cmd, String[] args, String line) {
        this.source = source;
        this.c = c;
        this.cmd = cmd;
        this.args = args;
        this.line = line;
    }

    public void run() {
        try {
            source.preExecuteCommand(c);
            c.testPermission(source, cmd, args, line);
            c.execute(source, cmd, args, line);
        } catch (Exception e) {
            source.onError(c, e);
        }
    }

}
