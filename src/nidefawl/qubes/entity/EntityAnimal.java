package nidefawl.qubes.entity;

import nidefawl.qubes.entity.ai.AITaskWander;

public abstract class EntityAnimal extends EntityAI {

    public EntityAnimal(boolean isServerEntity) {
        super(isServerEntity);
        this.properties = new EntityProperties();
        this.properties.setOption(0, this.random.nextInt(5));
        this.taskManager.add(new AITaskWander(this)).setPriority(1);
    }
}
