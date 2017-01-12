package nidefawl.qubes.entity;

import nidefawl.qubes.models.EntityModel;

public class EntityDemon extends EntityMob {
    
    public EntityDemon(boolean isServerEntity) {
        super(isServerEntity);

        this.properties = new EntityProperties();
        this.properties.setOption(0, this.random.nextInt(6));
        this.properties.setOption(1, this.random.nextInt(5));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEMON;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelDemon;
    }

}
