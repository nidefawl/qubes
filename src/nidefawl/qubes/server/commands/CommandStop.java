package nidefawl.qubes.server.commands;

public class CommandStop extends Command {

    public CommandStop() {
        super("stop");
        addAlias("halt");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        source.getServer().stopServer();
    }

    public boolean runSynchronized() {
        return false;
    }
}
