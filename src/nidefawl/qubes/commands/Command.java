package nidefawl.qubes.commands;

import java.util.HashSet;

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
}
