package nidefawl.qubes.chunk.client;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.world.World;

public class ChunkManagerClient extends ChunkManager {
    public ChunkManagerClient(World world) {
        super(world);
    }

    @Override
    protected ChunkTable makeChunkTable() {
        return new ChunkTable(MAX_CHUNK*2);
    }

}
