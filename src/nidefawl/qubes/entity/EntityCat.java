package nidefawl.qubes.entity;


import nidefawl.qubes.models.EntityModel;

public class EntityCat extends EntityAnimal {
    
    public EntityCat(boolean isServerEntity) {
        super(isServerEntity);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CAT;
    }
    
    @Override
    public EntityModel getEntityModel() {
        return EntityModel.modelCat;
    }
}
