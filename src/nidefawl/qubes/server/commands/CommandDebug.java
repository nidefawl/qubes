package nidefawl.qubes.server.commands;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.world.WorldServer;

public class CommandDebug extends Command {

    public CommandDebug() {
        super("debug");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        GameServer server = source.getServer();
        if (args.length > 0) {
            switch (args[0]) {
                case "deletechunks":
                    int n = ((WorldServer)source.getWorld()).deleteAllChunks();
                    source.sendMessage("Deleted "+n+" chunks");
                    return;
            }
        }
    }
}
