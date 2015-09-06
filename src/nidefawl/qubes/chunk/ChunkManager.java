package nidefawl.qubes.chunk;

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
    public abstract void startThreads();
    public abstract void onWorldUnload();

    public void saveAll() {
    }

    public void queueLoadChecked(long l) {
    }
}
