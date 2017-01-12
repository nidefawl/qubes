package nidefawl.qubes.server.commands;

import nidefawl.qubes.entity.*;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.world.WorldServer;

public class CommandEntity extends Command {

    public CommandEntity() {
        super("entity");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        checkArgs(args, 1, -1, "/entity <spawn|kill>");
        GameServer server = source.getServer();
        WorldServer w = source.getWorld();
        Player p = (PlayerServer) source;
        if ("kill".equals(args[0])) {
            for (int i = 0; i < w.entityList.size(); i++) {
                Entity e = w.entityList.get(i);
                if (e.getEntityType() == EntityType.PLAYER) {
                    continue;
                }
                if (e.getEntityType() == EntityType.PLAYER_SERVER) {
                    continue;
                }
                e.remove();
            }
        } else if ("spawnsome".equals(args[0])) {
            int t = 10;
            if (args.length > 1) {
                t= StringUtil.parseInt(args[1], t);
            }
            while (t > 0) {
                int n = p.getRandom().nextInt(23)+2;
                if (!EntityType.isValid(n)) {
                    continue;
                }
                Entity e = EntityType.newById(n, true);
                e.move(p.pos);
                w.addEntity(e);
                t--;
            }
        } else if ("spawn".equals(args[0])) {
            int t = 2;
            if (args.length > 1) {
                t= StringUtil.parseInt(args[1], t);
            }
            int n = 1;
            if (args.length > 2) {
                n= StringUtil.parseInt(args[2], n);
            }
            if (!EntityType.isValid(t)) {
                return;
            }
            for (int i = 0; i < n; i++) {
                Entity e = EntityType.newById(t, true);
                e.move(p.pos);
                w.addEntity(e);
            }
        }
    }
}
