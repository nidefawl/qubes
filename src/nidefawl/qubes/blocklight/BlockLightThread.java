package nidefawl.qubes.blocklight;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;

public class BlockLightThread extends Thread {
    private static long               sleepTime = 10;
    private LinkedBlockingQueue<Long> queue     = new LinkedBlockingQueue<Long>();
    final static int NUM_CACHES = 4;
    final LightChunkCache[] caches = new LightChunkCache[NUM_CACHES];
    int misses;
    int hits;
    private volatile boolean          isRunning;

    private WorldServer world;
    private boolean     finished;
    private final BlockLightUpdate lightUpdater;

    public BlockLightThread(WorldServer world) {
        setName("BlockLightThread");
        setDaemon(true);
        for (int i = 0; i < caches.length; i++) {
            caches[i] = new LightChunkCache();
        }
        this.isRunning = true;
        this.world = world;
        this.lightUpdater = new BlockLightUpdate();
    }

    public void init() {
        start();
    }

    @Override
    public void run() {
        try {

            System.out.println(getName() + " started");
            long last = 0L;
            HashSet<Long> work = new HashSet<>();
            while (world.getServer().isRunning() && this.isRunning) {
                boolean did = false;
                try {
                    int l = 0;
                    work.clear();
                    if ((l=this.queue.drainTo(work)) > 0) {
                        for (Long task : work) {
                            int x = getX(task);
                            int y = getY(task);
                            int z = getZ(task);
                            int flags = getFlags(task);
                            int type = flags&0x1;
                            if ((flags & 0x2) != 0) {
                                LightChunkCache cache = getCache(world, x >> 4, z >> 4);
                                if (cache != null)
                                    this.lightUpdater.updateBlock(cache, x, y, z, type);
                            } else {
                                LightChunkCache cache = getCache(world, x, z);
                                if (cache != null)
                                    this.lightUpdater.updateChunk(cache, x, z, type);
                            }
                            did = true;
                        }
                        for (int i = 0; i < NUM_CACHES; i++) {
                            caches[i].drainFlagged(world.getPlayerChunkTracker());    
                        }
                    }
                    long lT = System.currentTimeMillis()-last;
                    if (lT>=1000) {
                        last = System.currentTimeMillis();
                        int n = this.lightUpdater.numBlocksUpdate;
                        this.lightUpdater.numBlocksUpdate = 0;
                        float perSec = n*1000 / (float) lT;
                        System.out.printf("%.2f Light updates/s, Queue len %d, Cache: %d hits, %d misses\n", perSec, queue.size(), hits, misses);                            
                    }
                } catch (Exception e) {
                    ErrorHandler.setException(new GameError("Exception in " + getName(), e));
                    break;
                }
                if (!did) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        if (!isRunning)
                            break;
                        onInterruption();
                    }
                }
            }
            System.out.println(getName() + " ended");
        } finally {
            this.finished = true;
        }
    }

    /**
     * @param world2
     * @param i
     * @param j
     * @return
     */
    private LightChunkCache getCache(WorldServer world, int x, int z) {
        for (int i = 0; i < NUM_CACHES; i++) {
            if (this.caches[i].isValid(world, x, z)) {
                this.hits++;
                this.caches[i].flagUsed();
                return this.caches[i];
            }
        }
        this.misses++;
        LightChunkCache cache = null;
        for (int i = 0; i < NUM_CACHES; i++) {
            if (cache == null || this.caches[i].getNumUses() < cache.getNumUses()) {
                cache = this.caches[i];
            }
        }
        cache.drainFlagged(world.getPlayerChunkTracker());
        if (cache.cache(world, x, z))
            return cache;
        return null;
    }

    private void onInterruption() {
    }

    public void halt() {
        if (!this.finished) {
            this.isRunning = false;
            try {
                this.queue.clear();
                this.interrupt();
                Thread.sleep(60);
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!this.finished) {
                try {
                    this.queue.clear();
                    this.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(60);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static long toHash(long x, long y, long z, long i) {
        x += 0x7FFFFF;
        z += 0x7FFFFF;
        return (x & 0xFFFFFF) | ((z & 0xFFFFFF) << 24) | ((y & 0x1FF) << 48) | ((i& 0b111)<<(48+9));
    }

    public static int getX(long hash) {
        return (int) ((hash & 0xFFFFFFL) - 0x7FFFFFL);
    }

    public static int getZ(long hash) {
        return (int) (((hash >> 24) & 0xFFFFFFL) - 0x7FFFFFL);
    }

    public static int getY(long hash) {
        return (int) ((hash >> 48) & 0x1FFL);
    }

    public static int getFlags(long hash) {
        return (int) ((hash >> (48 + 9)) & 0b111);
    }

    public void queueBlock(int x, int y, int z, int i) {
        long l = toHash(x, y, z, 0b10|(i&0x1));
        this.queue.add(l);
    }

    public void queueChunk(int x, int z, int i) {
        long l = toHash(x, 0, z, 0b00|(i&0x1));
        this.queue.add(l);
    }
}