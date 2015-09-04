package nidefawl.qubes.commands;

import java.util.HashSet;

import nidefawl.qubes.util.StringUtil;

public class CommandHandler {
    HashSet<Command> commands = new HashSet<>();
    
    public CommandHandler() {
        registerBaseCommands();
    }

    private void registerBaseCommands() {
        this.register(CommandStop.class);
    }

    private void register(Class<? extends Command> class1) {
        try {
            Command c = (Command) class1.getConstructors()[0].newInstance();
            commands.add(c);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void handle(ICommandSource source, String line) {
        String[] args = line.split(" ");
        String cmd = args[0];
        args = StringUtil.dropArrIdx(args, 0);
        for (Command c : this.commands) {
            if (c.matches(cmd, args, line)) {
                executeCommand(source, c, cmd, args, line);
                return;
            }
        }
        source.onUnknownCommand(cmd, line);
    }

    private void executeCommand(ICommandSource source, Command c, String cmd, String[] args, String line) {
        try {
            source.preExecuteCommand(c);
            c.testPermission(source, cmd, args, line);
            c.execute(source, cmd, args, line);
        } catch (Exception e) {
            source.onError(c, e);
        }
    }

}
