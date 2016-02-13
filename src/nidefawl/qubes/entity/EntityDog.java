package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityDog extends EntityAnimal {
    
    public EntityDog() {
        super();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOG;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelDog;
    }
}
