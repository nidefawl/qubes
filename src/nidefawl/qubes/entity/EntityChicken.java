package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntityChicken extends EntityAnimal {
    
    public EntityChicken() {
        super();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CHICKEN;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelChicken;
    }
}
