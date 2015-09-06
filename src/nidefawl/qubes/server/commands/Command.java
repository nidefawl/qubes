package nidefawl.qubes.server.commands;

import java.util.HashSet;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.server.GameServer;

public abstract class Command {
    HashSet<String> aliases = new HashSet<>();
    final String    name;

    public Command(String name) {
        this.name = name;
        addAlias(this.name);
    }

    protected void addAlias(String alias) {
        this.aliases.add(alias);
    }

    public boolean matches(String command, String[] args, String line) {
        return this.aliases.contains(command);
    }

    public void testPermission(ICommandSource source, String cmd, String[] args, String line) {
    }

    public abstract void execute(ICommandSource source, String cmd, String[] args, String line);

    public String getName() {
        return this.name;
    }

    public boolean runSynchronized() {
        return true;
    }

    static void checkArgs(String[] args, int i, int j, String string) {
        if (args.length < i || (j >= 0 && args.length > j))
            throw new CommandException("Usage: "+string);
    }

    static Player matchPlayer(GameServer server, String string) {
        Player p = server.getPlayerManager().matchPlayer(string);
        if (p == null) {
            throw new CommandException("Player not found");
        }
        return p;
    }
}
