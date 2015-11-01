package nidefawl.qubes.meshing;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.Game;
import nidefawl.qubes.render.region.MeshedRegion;
//import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldClient;

public class MeshThread extends Thread {
    private static long                                 sleepTime = 10;
    private LinkedBlockingQueue<MeshUpdateTask> queue     = new LinkedBlockingQueue<MeshUpdateTask>();
    private LinkedList<MeshUpdateTask>          results   = new LinkedList<MeshUpdateTask>();
    private LinkedList<MeshUpdateTask>          finish    = new LinkedList<MeshUpdateTask>();
    private volatile boolean                            hasResults;
    private volatile boolean                            isRunning;
    private final MeshUpdateTask[]              tasks;

    public MeshThread(int numTasks) {
        setName("RegionRenderThread");
        setDaemon(true);
        this.isRunning = true;
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
        start();
    }

    @Override
    public void run() {
        while (Game.instance.isRunning() && this.isRunning) {
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
                if (!isRunning)
                    break;
                onInterruption();
            } catch (Exception e) {
                Game.instance.setException(new GameError("Exception in " + getName(), e));
                break;
            }
        }
        System.out.println("render thread ended");
    }

    private void onInterruption() {
        synchronized (this.results) {
            this.results.clear();
            this.hasResults = false;
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


    public boolean offer(WorldClient world, MeshedRegion m, int renderChunkX, int renderChunkZ) {
        MeshUpdateTask task = getNextTask();
        if (task != null) {
            if (task.prepare(world, m, renderChunkX, renderChunkZ)) {
                task.worldInstance = this.id;
                this.queue.add(task);
                tasksRunning++;
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
        id = -1;
        isRunning = false;
        this.interrupt(); // maybe it will end..
    }
    public boolean isRunning() {
        return this.isRunning;
    }
}
