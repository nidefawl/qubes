package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;


public class EntitySheep extends EntityAnimal {
    
    public EntitySheep() {
        super();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SHEEP;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelSheep;
    }
}
