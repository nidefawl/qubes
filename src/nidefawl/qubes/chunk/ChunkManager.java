package nidefawl.qubes.chunk;

import java.util.Collection;

import nidefawl.qubes.world.IChunkWorld;

public abstract class ChunkManager {
    public final static int MAX_CHUNK = 4000;

    public final IChunkWorld world;
    public final ChunkTable table;

    public ChunkManager(IChunkWorld world) {
        this.world = world;
        this.table = makeChunkTable();
    }

    protected abstract ChunkTable makeChunkTable();

    /**
     * 
     * @param x
     * @param z
     * @return null if the chunk isn't loaded
     */
    public Chunk get(int x, int z) {
        return this.table.get(x, z);
    }

    public int getChunksLoaded() {
        return this.table.size();
    }

    public void saveAll() {
    }

    public void queueLoadChecked(long l) {
    }

    /**
     * Deletes all chunks so world can regenerate
     * @return 
     */
    public int deleteAllChunks() {
        return 0;
    }
    public int regenChunks(Collection<Long> chunks) {
        return 0;
    }
}
