package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityGoat extends EntityAnimal {
    
    public EntityGoat(boolean isServerEntity) {
        super(isServerEntity);
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
