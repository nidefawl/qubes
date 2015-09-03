package nidefawl.qubes.world;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TestTerrain2;

public class WorldServer extends World {

//    private AbstractGen generator;
    public WorldServer(int worldId, long seed) {
        super(worldId, seed);
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

}
