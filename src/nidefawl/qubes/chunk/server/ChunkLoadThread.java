package nidefawl.qubes.chunk.server;

import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldServer;

public class ChunkLoadThread implements Runnable {
    private static long               sleepTime = 10;
    private LinkedBlockingQueue<Long> queue     = new LinkedBlockingQueue<Long>();
    private volatile boolean          isRunning;

    private ChunkManagerServer mgr;
    private boolean finished;
    private Thread threads[] = new Thread[4];

    public ChunkLoadThread(ChunkManagerServer mgr, WorldServer world) {
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(this);
            threads[i].setName("ChunkLoadThread_"+i+"_"+world.getName());
            threads[i].setDaemon(true);
        }
        this.isRunning = true;
        this.mgr = mgr;
    }

    public void startThreads() {
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
    }
    
    @Override
    public void run() {
        try {

            System.out.println(Thread.currentThread().getName() + " started");

            while (mgr.isRunning() && this.isRunning) {
                boolean did = false;
                try {
                    Long task = this.queue.take();
                    if (task != null) {
                        int x = GameMath.lhToX(task);
                        int z = GameMath.lhToZ(task);
                        mgr.loadOrGenerate(x, z);
                    }
                } catch (InterruptedException e1) {
                    if (!isRunning)
                        break;
                    onInterruption();
                } catch (Exception e) {
                    ErrorHandler.setException(new GameError("Exception in " + Thread.currentThread().getName(), e));
                    break;
                }
                if (!did && sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        if (!isRunning)
                            break;
                        onInterruption();
                    }
                }
            }
            System.out.println(Thread.currentThread().getName() + " ended");
        } finally {
            this.finished = true;
        }
    }

    private void onInterruption() {
    }

    /**
     * The thread might still be working on an item after calling this
     */
    public void ensureEmpty() {
        this.queue.clear();
        while ((mgr.isRunning() && this.isRunning) && !this.queue.isEmpty());
    }

    public void queueLoad(int x, int z) {
        this.queue.add(GameMath.toLong(x, z));
    }

    public void queueLoadChecked(long l) {
        if (!this.queue.contains(l)) {
            this.queue.add(l);
        }
    }

    public void halt() {
        if (!this.finished) {
            this.isRunning = false;
            try {
                this.queue.clear();
                for (int i = 0; i < threads.length; i++) {
                    threads[i].interrupt();
                }
                Thread.sleep(60);
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!this.finished) {
                try {
                    this.queue.clear();
                    for (int i = 0; i < threads.length; i++) {
                        threads[i].interrupt();
                    }
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
}
