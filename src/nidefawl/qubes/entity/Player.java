package nidefawl.qubes.entity;

public abstract class Player extends Entity {

    public String        name;
    public int punchTicks;
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
    @Override
    public void tickUpdate() {
        super.tickUpdate();
        updateTicks();
    }
    public void updateTicks() {
        if (this.punchTicks > 0) {
            this.punchTicks--;
        }
    }

}
