package nidefawl.qubes.chunk.server;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.world.World;

public class ChunkManagerServer extends ChunkManager {
    public ChunkManagerServer(World world) {
        super(world);
    }

    @Override
    protected ChunkTable makeChunkTable() {
        return new ChunkTable(MAX_CHUNK*2);
    }
}
