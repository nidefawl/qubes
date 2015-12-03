package nidefawl.qubes.entity;

public abstract class Player extends Entity {

    public String        name;
    public Player() {
        super();
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }
}
