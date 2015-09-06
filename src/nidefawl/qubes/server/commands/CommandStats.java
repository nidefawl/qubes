package nidefawl.qubes.server.commands;

import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.world.WorldServer;

public class CommandStats extends Command {

    public CommandStats() {
        super("stats");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        GameServer server = source.getServer();
        WorldServer[] worlds = server.getWorlds();;
        source.sendMessage(""+worlds.length+" loaded worlds");
        for (int i = 0; i < worlds.length; i++) {
            WorldServer w = worlds[i];
            int loadedchunks = w.getChunkManager().table.size();
            int playerChunkTracker = w.getPlayerChunkTracker().getSize();
            source.sendMessage("World "+w.getId()+": Loaded chunks "+loadedchunks+", Chunk trackers: "+playerChunkTracker);
            
        }
        
    }
}
