package nidefawl.qubes.meshing;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.Client;
import nidefawl.qubes.Main;
import nidefawl.qubes.render.region.MeshedRegion;
//import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.util.GameError;

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
        while (Main.instance.isRunning() && this.isRunning) {
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
                Main.instance.setException(new GameError("Exception in " + getName(), e));
                break;
            }
            try {
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                if (!isRunning)
                    break;
                onInterruption();
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

    public int finishTasks() {
        if (finish.isEmpty()) {
            if (!hasResults)
                return 0;
            synchronized (this.results) {
                this.finish.addAll(this.results);
                this.results.clear();
                this.hasResults = false;
            }
        }
        if (finish.size() > 0) {
            int num = 0;
            MeshUpdateTask w = this.finish.getFirst();
            if (w.finish(this.id)) {
                this.finish.removeFirst();
                w.worldInstance = 0;
                tasksRunning--;
            }
            num++;
            return num;
        }
        return 0;
    }


    public boolean offer(MeshedRegion m, int renderChunkX, int renderChunkZ) {
        MeshUpdateTask task = getNextTask();
        if (task != null) {
            if (task.prepare(m, renderChunkX, renderChunkZ)) {
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
}
