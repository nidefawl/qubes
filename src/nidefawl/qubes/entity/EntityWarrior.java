package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityWarrior extends EntityMob {
    
    public EntityWarrior(boolean isServerEntity) {
        super(isServerEntity);
        this.properties = new EntityProperties();
        for (int i = 0; i < 9; i++)
            this.properties.setOption(i, this.random.nextInt(3));
        this.properties.setOption(9, this.random.nextInt(2));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WARRIOR;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelWarrior;
    }
}
