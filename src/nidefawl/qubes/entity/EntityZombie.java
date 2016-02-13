package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityZombie extends EntityMob {
    
    public EntityZombie() {
        super();
        this.properties = new EntityProperties();
        this.properties.setOption(0, this.random.nextInt(5));
        this.properties.setOption(1, this.random.nextInt(3));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ZOMBIE;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelZombie;
    }
}
