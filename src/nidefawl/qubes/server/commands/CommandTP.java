package nidefawl.qubes.server.commands;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.server.GameServer;

public class CommandTP extends Command {

    public CommandTP() {
        super("tp");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        checkArgs(args, 1, -1, "/tp <playername>");
        GameServer server = source.getServer();
        if (args.length > 0 && source instanceof PlayerServer) {
            Player other = server.getPlayerManager().getPlayer(args[0]);
            if (other == null) {
                source.sendMessage("Player not found");
                return;
            }
            ((Player)source).move(other.pos);
        }
    }
}
