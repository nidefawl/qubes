package nidefawl.qubes.entity.ai;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.EntityAI;

public class TaskManager {

    public EntityAI entity;
    public List<TaskEntry> tasks = Lists.newArrayList();
    
    public TaskManager(EntityAI entity) {
        this.entity = entity;
    }
    
    public TaskEntry add(AITask task) {
        TaskEntry entry = new TaskEntry(task);
        entry.setEnabled(true);
        this.tasks.add(entry);
        return entry;
    }
    
    public void update() {
        for (int i = 0; i < tasks.size(); i++) {
            TaskEntry t = tasks.get(i);
            if (!t.isEnabled()) {
                t.setRunning(false);
                continue;
            }
            if (t.isExecuting()) {
                if (!canSchedule(t, tasks) || !t.keepExecuting()) {
                    t.stop();
                }
            } else if (canSchedule(t, tasks) && t.shouldExecute()) {
                t.start();
            }
            if (t.isExecuting()) {
                t.update();
            }
        }
    }
    
    private boolean canSchedule(TaskEntry t, List<TaskEntry> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            TaskEntry t2 = tasks.get(i);
            if (t2 != t || !t2.isEnabled() || !t2.isExecuting()) {
                continue;
            }
            if (t2.getPriority() > t.getPriority()) {
                continue;
            }
            if ((t2.getMask() & t.getMask()) != 0) {
                return false;
            }
        }
        return true;
    }
    
}
