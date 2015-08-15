package nidefawl.qubes.chunk;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.game.Main;
import nidefawl.qubes.util.GameError;

public class RegionLoaderThread extends Thread {
    private static long                         sleepTime = 10;
    private LinkedBlockingQueue<RegionLoadTask> queue     = new LinkedBlockingQueue<RegionLoadTask>();
    private LinkedList<RegionLoadTask>          results   = new LinkedList<RegionLoadTask>();
    private LinkedList<RegionLoadTask>          finish    = new LinkedList<RegionLoadTask>();
    private volatile boolean                    hasResults;
    private volatile boolean                    isRunning;
    private final RegionLoadTask[]              tasks;

    int tasksRunning = 0;
    int nextTask     = 0;

    public RegionLoaderThread(int numTasks) {
        setName("RegionLoaderThread");
        setDaemon(true);
        this.isRunning = true;
        this.tasks = new RegionLoadTask[numTasks];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new RegionLoadTask();
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
        System.out.println(getName() +" started");
        
        while (Main.instance.isRunning() && this.isRunning) {
            boolean did = false;
            try {
                RegionLoadTask task = this.queue.take();
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
        System.out.println(getName() +" ended");
    }

    private void onInterruption() {
        synchronized (this.results) {
            this.results.clear();
            this.hasResults = false;
        }
    }

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
            RegionLoadTask w = this.finish.getFirst();
            if (w.finish(this.id)) {
                this.finish.removeFirst();
                tasksRunning--;
            }
            num++;
            return num;
        }
        return 0;
    }

    public boolean offer(Region worldrenderer) {
        RegionLoadTask task = getNextTask();
        if (task != null) {
            if (task.prepare(worldrenderer)) {
                task.worldInstance = this.id;
                this.queue.add(task);
                nextTask++;
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

    private RegionLoadTask getNextTask() {
        if (tasksRunning >= this.tasks.length)
            return null;
        return tasks[nextTask % this.tasks.length];
    }

    int id = 0;

    public void flush() {
        id++;
    }

    public void stopThread() {
        id = -1;
        isRunning = false;
        this.interrupt(); // maybe it will end..
    }
}
