package nidefawl.qubes.world;

import java.io.File;

import nidefawl.qubes.GameServer;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;

public class WorldServer extends World {

    private File worldDirectory;
    private final GameServer server;

    //    private AbstractGen generator;
    public WorldServer(WorldSettings settings, GameServer server) {
        super(settings);
        this.worldDirectory = settings.getWorldDirectory();
        this.server = server;
        //        this.generator = new TestTerrain2(this, this.seed);
    }

    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerServer(this);
    }
    //    
    //    public AbstractGen getGenerator() {
    //        return generator;
    //    }

    public File getWorldDirectory() {
        return worldDirectory;
    }

    public GameServer getServer() {
        return this.server;
    }
}
