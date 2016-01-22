package nidefawl.qubes.chunk.server;

import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldServer;

public class ChunkLoadThread {
    private static long               sleepTime = 10;
    private LinkedBlockingQueue<Long> queue     = new LinkedBlockingQueue<Long>();

    private ChunkManagerServer mgr;
    private LoadThread threads[] = new LoadThread[4];
    static class LoadThread extends Thread {
        volatile boolean          isRunning = true;
        volatile boolean          finished = false;
        private final ChunkManagerServer mgr;
        private LinkedBlockingQueue<Long> queue;
        public LoadThread(ChunkLoadThread loader) {
            this.mgr=loader.mgr;
            this.queue=loader.queue;
        }

        @Override
        public void run() {
            try {

                System.out.println(Thread.currentThread().getName() + " started");

                while (mgr.isRunning() && this.isRunning) {
                    try {
                        Long task = this.queue.take();
                        if (task != null) {
                            int x = GameMath.lhToX(task);
                            int z = GameMath.lhToZ(task);
                            mgr.loadOrGenerate(x, z);
                        }
                    } catch (InterruptedException e1) {
                        break;
                    } catch (Exception e) {
                        ErrorHandler.setException(new GameError("Exception in " + Thread.currentThread().getName(), e));
                        break;
                    }
                }
                System.out.println(Thread.currentThread().getName() + " ended");
            } finally {
                this.isRunning = false;
                this.finished = true;
            }
        }

        public void halt() {
            if (!this.finished) {
                this.isRunning = false;
                try {
                    interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (!this.finished) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public ChunkLoadThread(ChunkManagerServer mgr, WorldServer world) {
        this.mgr = mgr;
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new LoadThread(this);
            threads[i].setName("ChunkLoadThread_"+i+"_"+world.getName());
            threads[i].setDaemon(true);
        }
    }

    public void startThreads() {
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
    }
    

    /**
     * The thread might still be working on an item after calling this
     */
    public void ensureEmpty() {
        this.queue.clear();
        while ((mgr.isRunning() && this.isRunning()) && !this.queue.isEmpty());
    }

    private boolean isRunning() {
        for (int i = 0; i < threads.length; i++) {
            if (threads[i]!=null&&threads[i].isRunning) {
                return true;
            }
        }
        return false;
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
        this.queue.clear();
        for (int i = 0; i < threads.length; i++) {
            threads[i].halt();
        }
    }

}
