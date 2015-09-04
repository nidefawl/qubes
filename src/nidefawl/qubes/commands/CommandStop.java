package nidefawl.qubes.commands;

public class CommandStop extends Command {

    public CommandStop() {
        super("stop");
        addAlias("halt");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        source.getServer().halt();
    }
}
