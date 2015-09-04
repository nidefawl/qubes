package nidefawl.qubes.chunk.server;

import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class ChunkLoadThread extends Thread {
    private static long               sleepTime = 10;
    private LinkedBlockingQueue<Long> queue     = new LinkedBlockingQueue<Long>();
    private volatile boolean          isRunning;

    private ChunkManagerServer mgr;
    private boolean finished;

    public ChunkLoadThread(ChunkManagerServer mgr) {
        setName("ChunkLoadThread");
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

            while (mgr.isRunning() && this.isRunning) {
                boolean did = false;
                try {
                    Long task = this.queue.take();
                    if (task != null) {
                        synchronized (this.mgr.syncObj) {
                            Chunk c = mgr.table.get(task);
                            if (c == null) {
                                int x = GameMath.lhToX(task);
                                int z = GameMath.lhToZ(task);
                                mgr.loadOrGenerate(x, z);
                            }
                        }
                    }
                } catch (InterruptedException e1) {
                    if (!isRunning)
                        break;
                    onInterruption();
                } catch (Exception e) {
                    Game.instance.setException(new GameError("Exception in " + getName(), e));
                    break;
                }
                try {
                    if (sleepTime > 0) {
                        Thread.sleep(did ? sleepTime : sleepTime * 3);
                    }
                } catch (InterruptedException e) {
                    if (!isRunning)
                        break;
                    onInterruption();
                }
            }
            System.out.println(getName() + " ended");
        } finally {
            this.finished = true;
        }
    }

    private void onInterruption() {
    }

    public void queueLoad(int x, int z) {
        this.queue.add(GameMath.toLong(x, z));
    }

    public void queueLoadChecked(int x, int z) {
        long l = GameMath.toLong(x, z);
        if (!this.queue.contains(l)) {
            this.queue.add(l);
        }
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
}
