package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityDuck extends EntityAnimal {
    
    public EntityDuck(boolean isServerEntity) {
        super(isServerEntity);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DUCK;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelDuck;
    }
}
