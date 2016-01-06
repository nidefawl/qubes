package nidefawl.qubes.chunk.server;

import java.io.File;
import java.io.IOException;
import java.util.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.ChunkTable;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ServerStats;
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
        this.thread = new ChunkLoadThread(this, world);
        this.unloadThread = new ChunkUnloadThread(this);
        this.reader = new ChunkReader(this, this.regionFileCache);
    }
    public void startThreads() {
        this.thread.startThreads();
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
        Chunk cExist = table.get(x, z);
        Chunk cLoad = null;
        if (cExist == null) {
            boolean wasLoaded = true;
            cLoad = this.reader.loadChunk(this.world, x, z);
            if (cLoad == null) {
                wasLoaded = false;
                ITerrainGen gen = this.worldServer.getGenerator();
                long l1 = System.nanoTime();
                cLoad = gen.generateChunk(x, z);
                long l2 = System.nanoTime();
                ServerStats.add("generateChunk", l2-l1);
                ServerStats.add("postGenerate", System.nanoTime()-l2);
                ServerStats.add("generatedChunks", 1);
            }
            synchronized (this.syncObj2) {
                Chunk cExist2 = table.get(x, z);
                if (cExist2 == null) {
                    this.table.put(x, z, cLoad);
                    if (wasLoaded) {
                        cLoad.postLoad();
                    } else {
                        cLoad.postGenerate();
                    }
                }
            }
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
    public int regenChunks(Collection<Long> chunks) {
        this.thread.ensureEmpty();
        this.unloadThread.ensureEmpty();
        int n = 0;
        for (Long l : chunks) {
            Chunk c = this.table.remove(l);
            if (c != null) {
                c.isValid = false;
                c.isUnloading = true;
                n++;
            }
        }
        for (Long l : chunks) {
            int x = GameMath.lhToX(l); int z = GameMath.lhToZ(l);
            RegionFile rf = regionFileCache.getRegionFileChunk(x, z);
            try {
                rf.writeChunk(x, z, new byte[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Long l : chunks) {
            this.queueLoadChecked(l);
        }
        return n;
    }
}
