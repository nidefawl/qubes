package nidefawl.qubes.chunk.server;

import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class ChunkUnloadThread extends Thread {
    private static long               sleepTime = 10;
    private LinkedBlockingQueue<Long> queue     = new LinkedBlockingQueue<Long>();
    private volatile boolean          isRunning;

    private ChunkManagerServer mgr;
    private boolean finished;

    public ChunkUnloadThread(ChunkManagerServer mgr) {
        setName("ChunkUnloadThread");
        setDaemon(true);
        this.isRunning = true;
        this.mgr = mgr;
    }

    public void init() {
        start();
    }

    @Override
    public void run() {
        try {
            System.out.println(getName() + " started");
            while ((mgr.isRunning() && this.isRunning) || !this.queue.isEmpty()) {
                try {
                    Long task = this.queue.take();
                    if (task != null) {
                        synchronized (this.mgr.syncObj) {
                            Chunk c = mgr.table.get(task);
                            if (c != null) {
                                PlayerChunkTracker tracker = mgr.worldServer.getPlayerChunkTracker();
                                int x = GameMath.lhToX(task);
                                int z = GameMath.lhToZ(task);
                                if (!tracker.isRequired(x, z)) {
                                    mgr.unloadChunk(x, z);
                                } else {
                                    c.isUnloading = false;
                                }
                            }
                        }
                    }
                } catch (InterruptedException e1) {
                    break;
                } catch (Exception e) {
                    ErrorHandler.setException(new GameError("Exception in " + getName(), e));
                    break;
                }
            }
            System.out.println(getName() + " ended");
        } finally {
            this.isRunning = false;
            this.finished = true;
        }
    }

    /**
     * The thread might still be working on an item after calling this
     */
    public void ensureEmpty() {
        this.queue.clear();
        while ((mgr.isRunning() && this.isRunning) && !this.queue.isEmpty());
    }


    public void queueUnloadChecked(Long l) {
        if (!this.isRunning) {
            System.err.println("Attempt to queue unload after thread halt");
            Thread.dumpStack();
            return;
        }
        if (!this.queue.contains(l)) {
            this.queue.add(l);
        }
    }

    public void halt() {
        if (!this.finished) {
            this.isRunning = false;
            try {
                if (!this.queue.isEmpty()) {
                    System.out.println("Waiting for "+this.queue.size()+" chunks to unload...");
                }
                while (!this.queue.isEmpty()) {
                    Thread.sleep(100);
                }
                while (!this.finished) {
                    if (this.queue.isEmpty())
                        this.interrupt();
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
