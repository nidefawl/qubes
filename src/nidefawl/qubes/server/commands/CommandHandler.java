package nidefawl.qubes.server.commands;

import java.util.HashSet;

import nidefawl.qubes.util.StringUtil;

public class CommandHandler {
    HashSet<Command> commands = new HashSet<>();
    
    public CommandHandler() {
        registerBaseCommands();
    }

    private void registerBaseCommands() {
        this.register(CommandStop.class);
        this.register(CommandSave.class);
        this.register(CommandKick.class);
        this.register(CommandList.class);
        this.register(CommandStats.class);
        this.register(CommandSetTime.class);
        this.register(CommandDebug.class);
        this.register(CommandToCoord.class);
    }

    public void register(Class<? extends Command> class1) {
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
            if (c.runSynchronized()) {
                source.getServer().commandQueue.add(new PreparedCommand(source, c, cmd, args, line));
            } else {

                source.preExecuteCommand(c);
                c.testPermission(source, cmd, args, line);
                c.execute(source, cmd, args, line);
            }
        } catch (CommandException e) {
            source.onError(c, e);
        } catch (Exception e) {
            source.onError(c, new CommandException("Error: "+e.getMessage(), e));
        }
    }

}
