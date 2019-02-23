package nidefawl.qubes.meshing;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.render.region.MeshedRegion;
//import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.WorldClient;

public class MeshThread implements Runnable {
    private static long                                 sleepTime = 10;
    private LinkedBlockingQueue<MeshUpdateTask> queue     = new LinkedBlockingQueue<MeshUpdateTask>();
    private LinkedList<MeshUpdateTask>          results   = new LinkedList<MeshUpdateTask>();
    private LinkedList<MeshUpdateTask>          finish    = new LinkedList<MeshUpdateTask>();
    private volatile boolean                            hasResults;
    private volatile boolean                            isRunning;
    private volatile boolean                            finished;
    private final MeshUpdateTask[]              tasks;
    final Thread[] threads;
    public MeshThread(int numTasks) {
        this.isRunning = true;
        this.threads = new Thread[numTasks];
        this.tasks = new MeshUpdateTask[numTasks];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new MeshUpdateTask();
        }
        if (numTasks > 3) {
            sleepTime = 0;
        } else {
            sleepTime = 10;
        }
    }

    public void init() {
        for (int i = 0; i < this.threads.length; i++) {
            if (this.threads[i] != null) {
                this.threads[i].interrupt();
            }
            this.threads[i] = new Thread(this);
            this.threads[i].setName("RegionRenderThread #"+i);
            this.threads[i].setDaemon(true);
        }
        for (int i = 0; i < this.threads.length; i++) {
            this.threads[i].start();
        }
    }

    @Override
    public void run() {
        try {
            while (GameBase.baseInstance.isRunning() && this.isRunning) {
                boolean did = false;
                try {
                    MeshUpdateTask task = this.queue.take();
                    if (task != null) {
                        if (task.isValid(this.id)) {
                            did = task.updateFromThread();
                        }
                        synchronized (this.results) {
                            hasResults = true;
                            results.add(task);
                        }
                    }
                } catch (InterruptedException e1) {
                    break;
                } catch (Exception e) {
                    GameBase.baseInstance.setException(new GameError("Exception in " + Thread.currentThread().getName(), e));
                    break;
                }
                if (Thread.interrupted()) {
                    break;
                }
            }
            synchronized (this.results) {
                this.results.clear();
                this.hasResults = false;
            }
            System.out.println("render thread ended");
        } finally {
            isRunning = false;
            finished = true;
        }
    }


    int tasksRunning = 0;

    public MeshedRegion finishTask() {
        MeshedRegion m = null;
        if (finish.isEmpty() && hasResults) {
            synchronized (this.results) {
                this.finish.addAll(this.results);
                this.results.clear();
                this.hasResults = false;
            }
        }
        if (finish.size() > 0) {
            MeshUpdateTask w = this.finish.getFirst();
            if (w.finish(this.id)) {
                this.finish.removeFirst();
                w.worldInstance = 0;
                m = w.getRegion();
                tasksRunning--;
            }
        }
        return m;
    }


    public boolean offer(IBlockWorld world, ChunkManager mgr, MeshedRegion m, int renderChunkX, int renderChunkZ) {
        MeshUpdateTask task = getNextTask();
        if (task != null) {
            if (task.prepare(world, mgr, m, renderChunkX, renderChunkZ)) {
                task.worldInstance = this.id;
                tasksRunning++;
                this.queue.add(task);
                return true;
            }
        }
        return false;
    }

    public boolean busy() {
        return tasksRunning >= this.tasks.length;
    }

    public boolean hasTasks() {
        return tasksRunning > 0;
    }

    private MeshUpdateTask getNextTask() {
        if (tasksRunning >= this.tasks.length)
            return null;
        for (int i = 0; i < this.tasks.length; i++)
            if (tasks[i].worldInstance == 0)
                return tasks[i];
        return null;
    }

    int id = 1;

    public void flush() {
        id++;
    }

    public void stopThread() {
        if (!this.finished) {
            try {
                this.id = -1;
                this.isRunning = false;
                this.queue.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < this.threads.length; i++) {
                if (this.threads[i] != null) {
                    this.threads[i].interrupt();
                }
            }
            while (!this.finished) {
                try {
                    Thread.sleep(60);
                    for (int i = 0; i < this.threads.length; i++) {
                        if (this.threads[i] != null) {
                            this.threads[i].interrupt();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void cleanup() {
        this.queue.clear();
        this.results.clear();
        this.finish.clear();
        this.hasResults = false;
        for (int i = 0; i < tasks.length; i++) {
            tasks[i].destroy();
        }
    }
}
