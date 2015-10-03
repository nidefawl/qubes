package nidefawl.qubes.server.commands;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.util.StringUtil;

public class CommandToCoord extends Command {

    public CommandToCoord() {
        super("tocoord");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        if (args.length > 2 && source instanceof Player) {
            int x = StringUtil.parseInt(args[0], 0);
            int y = StringUtil.parseInt(args[1], 255);
            int z = StringUtil.parseInt(args[2], 0);
            ((Player)source).move(x, y, z);
        }
    }
}
