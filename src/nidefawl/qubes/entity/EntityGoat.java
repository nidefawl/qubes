package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityGoat extends EntityAnimal {
    
    public EntityGoat() {
        super();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GOAT;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelGoat;
    }
}
