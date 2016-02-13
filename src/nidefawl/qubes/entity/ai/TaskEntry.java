package nidefawl.qubes.entity.ai;

public class TaskEntry {

    private AITask task;
    private int priority;
    private int mask;
    private boolean running;
    private boolean enabled;

    public TaskEntry(AITask task) {
        this.task = task;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return this.mask;
    }
    public boolean isRunning() {
        return this.running;
    }
    public void setRunning(boolean running) {
        this.running = running;
    }
    public int getPriority() {
        return this.priority;
    }
    public boolean isEnabled() {
        return this.enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExecuting() {
        return this.running;
    }

    public boolean shouldExecute() {
        return this.task.shouldExecute();
    }

    public boolean keepExecuting() {
        return this.task.keepExecuting();
    }

    public void stop() {
        this.running = false;
        this.task.stop();
    }

    public void start() {
        this.running = true;
        this.task.start();
    }

    public void update() {
        this.task.update();
    }
}
