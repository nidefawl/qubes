/**
 * 
 */
package nidefawl.qubes.entity;

import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Side;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class EntityType {
    private final static EntityType[] mapping = new EntityType[255];
    /**
     * @param i
     * @param class1
     */
    public final int id;
    public final Class<? extends Entity> clazz;
    private Class<? extends Entity> clientClass;
    public EntityType(int id, Class<? extends Entity> clazz) {
        this(id, clazz, clazz);
    }
    public EntityType(int id, Class<? extends Entity> clazz, Class<? extends Entity> clientClass) {
        mapping[id] = this;
        this.id = id;
        this.clazz = clazz;
        this.clientClass = clientClass;
    }

    public static EntityType PLAYER = new EntityType(1, PlayerServer.class, PlayerRemote.class);
    
    public static Entity newById(int id) {
        EntityType type = mapping[id];
        if (type == null) {
            throw new GameError("Invalid entity type id "+id);
        }
        return type.newInstance();
    }

    public Entity newInstance() {
        try {
            Class<? extends Entity> c;
            if (GameContext.getSide() == Side.CLIENT) {
                c = this.clientClass;
            } else {
                c = this.clazz;
            }
            return c.newInstance();
        } catch (Exception e) {
            throw new GameError("Failed spawning entity", e);
        }
    }
}
