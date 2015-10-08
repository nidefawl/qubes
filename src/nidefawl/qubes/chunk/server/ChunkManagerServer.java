package nidefawl.qubes.chunk.server;

import java.io.File;
import java.util.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.noise.NoiseLib;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

public class ChunkManagerServer extends ChunkManager {
    final ChunkLoadThread thread;
    final ChunkUnloadThread unloadThread;
    final ChunkReader reader;
    private RegionFileCache regionFileCache;
    public final Object syncObj = new Object();
    public final Object syncObj2 = new Object();
    WorldServer worldServer;

    public ChunkManagerServer(WorldServer world, File worldDirectory) {
        super(world);
        this.worldServer = world;
        this.regionFileCache = new RegionFileCache(new File(worldDirectory, "data"));
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

    int ntotal = 0;
    public void loadOrGenerate(int x, int z) {
        synchronized (this.syncObj2) {
            Chunk c = this.reader.loadChunk(this.world, x, z);
            if (c == null) {
                ITerrainGen gen = this.worldServer.getGenerator();
                long l = System.nanoTime();
                c = gen.generateChunk(x, z);
//                long chunkGenTIme = (System.nanoTime()-l);
//                System.out.println(chunkGenTIme);
                c.postGenerate();
                Stats.timeWorldGen += (System.nanoTime()-l) / 1000000.0D;
//                ntotal++;
//                if (ntotal%10==0) {
//                    if (ntotal > 1000) {
//                        ntotal = 0;
//                        Stats.timeWorldGen = 0;
//                    }
//                    double per = Stats.timeWorldGen/ntotal;
//                    System.out.printf("%d chunks generated (NOISEGEN = "+(NoiseLib.isLibPresent()?"C":"Java")+") (%.2fms/chunk)\n", ntotal, per);
//                }
            } else {
                c.postLoad();
            }
            this.table.put(x, z, c);
        }
    }
    public void queueLoad(int x, int z) {
        this.thread.queueLoad(x, z);
    }
    public void queueLoadChecked(int x, int z) {
        this.thread.queueLoadChecked(GameMath.toLong(x, z));
    }
    public void queueLoadChecked(long l) {
        Chunk c = this.table.get(l);
        if (c == null) {
            this.thread.queueLoadChecked(l);   
        }
    }

    Iterator<Chunk> it = null;
    public void saveAndUnloadChunks(int max) {
        Iterator<Chunk> it = this.it;
        if (it == null || !it.hasNext()) {
            it = this.it = this.table.iterator();
        }
        PlayerChunkTracker tracker = this.worldServer.getPlayerChunkTracker();
        int saved = 0;
        while (it.hasNext() && saved < max) {
            Chunk c = it.next();
            if (c.justLoaded()) {
                continue;
            }
            if (!tracker.isRequired(c.x, c.z)) {
                c.isUnloading = true;
                this.unloadThread.queueUnloadChecked(GameMath.toLong(c.x, c.z));
            } else if (c.needsSave) {
                saveChunk(c);
            }
            saved++;
        }
    }
    public void unloadChunk(int x, int z) {
        Chunk c = this.table.remove(x, z);
        if (c != null) {
            c.isValid = false;
            saveChunk(c);
        }
    }
    private void saveChunk(Chunk c) {
        synchronized (this.syncObj2) {
            this.reader.saveChunk(c);
        }
    }
    public void saveChunk(int x, int z) {
        Chunk c = this.table.get(x, z);
        if (c != null)
            saveChunk(c);
    }
    public void saveAll() {
        synchronized (this.syncObj) {
            synchronized (this.syncObj2) {
                List<Chunk> l = this.table.asList();
                for (int i = 0; i < l.size(); i++) {
                    Chunk c = l.get(i);
                    if (c.needsSave) {
                        saveChunk(c);
                        c.needsSave = false;
                    }
                }
            }
        }
    }
    public boolean isRunning() {
        return ((WorldServer)this.world).getServer().isRunning();
    }
    /**
     * @return
     */
    public Iterator<Chunk> newUpdateIterator() {
        return this.table.iterator();
    }
    /**
     * Deletes all chunks so world can regenerate
     * @return the number of chunks deleted
     */
    public int deleteAllChunks() {
        this.thread.ensureEmpty();
        this.unloadThread.ensureEmpty();
        Chunk[][] c = this.table.clear();
        for (int x = 0; x < c.length; x++) {
            Chunk[] zRow = c[x];
            if (zRow != null) {
                for (int z = 0; z < zRow.length; z++) {
                    Chunk chunk = zRow[z];
                    if (chunk != null) {
                        chunk.isValid = false;
                        chunk.isUnloading = true;
                    }
                }
            }
        }
        int n = this.reader.fileCache.deleteChunks();
        return n;
    }
}
