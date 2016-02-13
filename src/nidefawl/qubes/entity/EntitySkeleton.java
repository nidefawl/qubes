package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntitySkeleton extends EntityMob {
    
    public EntitySkeleton() {
        super();
        this.properties = new EntityProperties();
        this.properties.setOption(0, this.random.nextInt(10));
        this.properties.setOption(1, this.random.nextInt(4));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SKELETON;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelSkeleton;
    }
}
