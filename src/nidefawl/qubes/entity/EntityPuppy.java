package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityPuppy extends EntityAnimal {
    
    public EntityPuppy(boolean isServerEntity) {
        super(isServerEntity);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PUPPY;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelPuppy;
    }
}
