package nidefawl.qubes.world;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;

public class WorldServer extends World {

    public WorldServer(int worldId, long seed) {
        super(worldId, seed);
    }

    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerServer(this);
    }

}
