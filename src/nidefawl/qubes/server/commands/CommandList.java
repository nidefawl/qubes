package nidefawl.qubes.server.commands;

import nidefawl.qubes.network.ReaderThread;
import nidefawl.qubes.network.WriterThread;
import nidefawl.qubes.network.server.NetworkServer;
import nidefawl.qubes.network.server.ServerHandlerPlay;

public class CommandList extends Command {

    public CommandList() {
        super("list");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        NetworkServer server = source.getServer().getNetwork();
        int numLogin = server.getLoginHandlers().size();
        int numPlay = server.getHandlers().size();
        source.sendMessage(String.format("%d active login handlers", numLogin));
        source.sendMessage(String.format("%d active play handlers", numPlay));
        source.sendMessage(String.format("%d read thread, %d write threads", ReaderThread.ACTIVE_THREADS, WriterThread.ACTIVE_THREADS));
        for (ServerHandlerPlay handler : server.getHandlers()) {
            source.sendMessage("Player '"+handler.getPlayer().getName()+"' IP: "+handler.getAddr());
        }
    }
}
