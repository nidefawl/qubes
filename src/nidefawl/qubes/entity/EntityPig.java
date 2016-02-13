package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityPig extends EntityAnimal {
    
    public EntityPig() {
        super();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PIG;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelPig;
    }
}
