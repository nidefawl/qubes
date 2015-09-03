package nidefawl.qubes.chunk.server;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.world.World;
import nidefawl.qubes.worldgen.AbstractGen;

public class ChunkManagerServer extends ChunkManager {
    final ChunkLoadThread thread;

    public ChunkManagerServer(World world) {
        super(world);
        this.thread = new ChunkLoadThread(this);
    }
    public void startThreads() {

        this.thread.start();
    }
    public void stopThreads() {
        this.thread.halt();
    }

    @Override
    protected ChunkTable makeChunkTable() {
        return new ChunkTable(MAX_CHUNK * 2);
    }

    public void loadOrGenerate(int x, int z) {
        AbstractGen gen = world.getGenerator();
        Chunk c = gen.generateChunk(x, z);
        this.table.put(x, z, c);
    }
    public void queueLoad(int x, int z) {
        this.thread.queueLoad(x, z);
    }
    public void queueLoadChecked(int x, int z) {
        this.thread.queueLoadChecked(x, z);
    }

    public void ensureLoaded(int xPosC, int zPosC, int halflen) {
        for (int x = -halflen; x <= halflen; x++) {
            for (int z = -halflen; z <= halflen; z++) {
                Chunk c = this.table.get(xPosC+x, zPosC+z);
                if (c == null) {
                    this.queueLoadChecked(xPosC+x, zPosC+z);
                }
            }
        }
        // TODO Auto-generated method stub
        
    }
}
