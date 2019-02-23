package nidefawl.qubes.chunk.client;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.world.IChunkWorld;

public class ChunkManagerClient extends ChunkManager {
    public ChunkManagerClient(IChunkWorld world) {
        super(world);
    }

    @Override
    protected ChunkTable makeChunkTable() {
        return new ChunkTable(MAX_CHUNK*2);
    }

    public Chunk getOrMake(int x, int z) {
        Chunk c = this.table.get(x, z);
        if (c == null) {
            c = new Chunk(this.world, x, z);
            c.world = this.world;
            this.table.put(x, z, c);
        }
        return c;
    }

    /**
     * @param x
     * @param z
     */
    public void remove(int x, int z) {
        Chunk c = this.table.remove(x, z);
        if (c != null) {
            c.isValid = false;
        }
    }

}
