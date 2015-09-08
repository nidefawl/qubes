package nidefawl.qubes.chunk.client;

import nidefawl.qubes.chunk.Chunk;
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

    public Chunk getOrMake(int x, int z) {
        Chunk c = this.table.get(x, z);
        if (c == null) {
            c = new Chunk(this.world, x, z, this.world.worldHeightBits);
            c.world = this.world;
            this.table.put(x, z, c);
        }
        return c;
    }

}
