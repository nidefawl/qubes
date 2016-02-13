package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityPuppy extends EntityAnimal {
    
    public EntityPuppy() {
        super();
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
