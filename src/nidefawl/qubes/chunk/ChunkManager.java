package nidefawl.qubes.chunk;

import nidefawl.qubes.meshing.ChunkRenderCache;
import nidefawl.qubes.world.World;

public abstract class ChunkManager {
    public final static int MAX_CHUNK = 4000;

    public final World world;
    public final ChunkTable table;

    public ChunkManager(World world) {
        this.world = world;
        this.table = makeChunkTable();
    }

    protected abstract ChunkTable makeChunkTable();

    public Chunk get(int x, int z) {
        return null;
    }

    public int getChunksLoaded() {
        return this.table.approxSize();
    }
}
