package nidefawl.qubes.server.commands;

import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.world.IWorldSettings;
import nidefawl.qubes.world.WorldServer;

public class CommandSetTime extends Command {

    public CommandSetTime() {
        super("time");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        WorldServer w = (WorldServer) source.getWorld();
        IWorldSettings settings = w.getSettings();
        long timeProv = -1; 
        if (args.length > 0 && (args[0].equals("set") || (timeProv = StringUtil.parseLong(args[0], -1, 10)) > 0L)) {
            if (args.length > 1) {
                long l = timeProv > 0 ? timeProv : StringUtil.parseLong(args[1], -1, 10);
                if (l < 0) {
                    source.sendMessage("Provide a valid time please");
                    return;
                }
                settings.setTime(l);
                w.resyncTime();
            } else {
                showTime(source);
            }
        } else if (args.length > 0 && args[0].equals("len")) {
            if (args.length > 1) {
                long l = StringUtil.parseLong(args[1], -1, 10);
                if (l < 0) {
                    source.sendMessage("Provide a valid number please");
                    return;
                }
                settings.setDayLen(l);
                w.resyncTime();
            } else {
                showTime(source);
            }
        } else if (args.length > 0 && args[0].equals("fixed")) {
            settings.setFixedTime(!settings.isFixedTime());
            source.sendMessage("World time is now " + (settings.isFixedTime() ? "fixed" : "no longer fixed"));
            return;
        } else {
            showTime(source);
        }
    }

    /**
     * @param source
     */
    private void showTime(ICommandSource source) {
        WorldServer w = (WorldServer) source.getWorld();
        long l = w.getTime();
        long l2 = w.getDayTime();
        long l3 = w.getDayLength();
        source.sendMessage(String.format("Current time in world %s: %d (Daytime: %d, Daylength: %d)", w.getName(), l, l2, l3));
        return;
    }
}
