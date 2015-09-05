package nidefawl.qubes.server.commands;

public class CommandSave extends Command {

    public CommandSave() {
        super("save");
        addAlias("saveall");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        long start = System.nanoTime();
        source.getServer().save(args.length>0&&(args[0].equalsIgnoreCase("force")||args[0].equalsIgnoreCase("all")||args[0].equalsIgnoreCase("true")));
        long end = System.nanoTime();
        long took = end-start;
        source.sendMessage(String.format("Save done. Took %.2fs", took/1000000000.0f));
    }
}
