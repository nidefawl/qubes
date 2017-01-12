package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityPony extends EntityAnimal {
    
    public EntityPony(boolean isServerEntity) {
        super(isServerEntity);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PONY;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelPony;
    }
}
