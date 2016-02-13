package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityPony extends EntityAnimal {
    
    public EntityPony() {
        super();
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
