package nidefawl.qubes.entity.ai;

import nidefawl.qubes.entity.EntityAI;

public abstract class AITask {

    protected EntityAI entity;

    public AITask(EntityAI entity) {
        this.entity = entity;
    }

    public boolean shouldExecute() {
        return false;
    }

    public boolean keepExecuting() {
        return false;
    }

    public void stop() {
    }

    public void start() {
    }

    public void update() {
    }
}
