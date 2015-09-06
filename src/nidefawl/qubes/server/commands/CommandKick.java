package nidefawl.qubes.server.commands;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.StringUtil;

public class CommandKick extends Command {

    public CommandKick() {
        super("kick");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        checkArgs(args, 1, -1, "/kick <playername> <reason>");
        GameServer server = source.getServer();
        Player p = matchPlayer(server, args[0]);
        String msg = StringUtil.combine(args, 1, args.length);
        if (msg.isEmpty()) {
            msg = "You were kicked";
        }
        p.kick(msg);
        source.sendMessage("Kicked player "+p.getName());
    }
}
