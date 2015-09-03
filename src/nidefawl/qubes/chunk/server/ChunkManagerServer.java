package nidefawl.qubes.chunk.server;

import java.io.File;
import java.util.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;
import nidefawl.qubes.worldgen.AbstractGen;

public class ChunkManagerServer extends ChunkManager {
    final ChunkLoadThread thread;
    final ChunkUnloadThread unloadThread;
    final ChunkReader reader;
    private RegionFileCache regionFileCache;
    public final Object syncObj = new Object();
    public final Object syncObj2 = new Object();

    public ChunkManagerServer(World world) {
        super(world);
        this.regionFileCache = new RegionFileCache(new File(WorkingEnv.getWorldsFolder(), "test"));
        this.thread = new ChunkLoadThread(this);
        this.unloadThread = new ChunkUnloadThread(this);
        this.reader = new ChunkReader(this, this.regionFileCache);
    }
    public void startThreads() {

        this.thread.start();
        this.unloadThread.start();
    }
    public void onWorldUnload() {
        this.thread.halt();
        this.unloadThread.halt();
        List<Chunk> l = this.table.asList();
        for (int i = 0; i < l.size(); i++) {
            Chunk c = l.get(i);
            unloadChunk(c.x, c.z);
        }
        this.regionFileCache.closeAll();
    }

    @Override
    protected ChunkTable makeChunkTable() {
        return new ChunkTable(MAX_CHUNK * 2);
    }

    public void loadOrGenerate(int x, int z) {
        synchronized (this.syncObj2) {
            Chunk c = this.reader.loadChunk(this.world, x, z);
            if (c == null) {
                AbstractGen gen = world.getGenerator();
                long l = System.nanoTime();
                c = gen.generateChunk(x, z);
                Stats.timeWorldGen += (System.nanoTime()-l) / 1000000.0D;
            }
            this.table.put(x, z, c);
        }
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
    public void unloadUnused(int xPosC, int zPosC, int halflen) {
        HashSet<Long> toUnload = null;
        Iterator<Chunk> it = this.table.iterator();
        while (it.hasNext()) {
            Chunk c = it.next();
            if (c.justLoaded()) {
                continue;
            }
            if (c.x < xPosC-halflen || c.x > xPosC+halflen || c.z < zPosC-halflen || c.z > zPosC+halflen) {
                if (toUnload == null) toUnload = new HashSet<>();
                toUnload.add(GameMath.toLong(c.x, c.z));
            }
        }
        if (toUnload != null) { 
            for (Long l : toUnload) {
                this.unloadThread.queueUnloadChecked(l);
            }
        }
    }
    public void unloadChunk(int x, int z) {
        Chunk c = this.table.remove(x, z);
        if (c != null) {
            System.out.println("unloading "+x+"/"+z);
            synchronized (this.syncObj2) {
                this.reader.saveChunk(c);
            }
        }
    }
}
