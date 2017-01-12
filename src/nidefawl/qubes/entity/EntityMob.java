package nidefawl.qubes.entity;


import nidefawl.qubes.entity.ai.AITaskWander;


public abstract class EntityMob extends EntityAI {
    
    public EntityMob(boolean isServerEntity) {
        super(isServerEntity);
        this.taskManager.add(new AITaskWander(this)).setPriority(1);
    }
}
