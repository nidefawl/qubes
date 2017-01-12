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
    public static EntityType PLAYER;
    public static EntityType PLAYER_SERVER;
    public static EntityType CAT;
    public static EntityType CHICKEN;
    public static EntityType DOG;
    public static EntityType DUCK;
    public static EntityType GOAT;
    public static EntityType PIG;
    public static EntityType PONY;
    public static EntityType PUPPY;
    public static EntityType SHEEP;
    public static EntityType SKELETON;
    public static EntityType ZOMBIE;
    public static EntityType ARCHER;
    public static EntityType WARRIOR;
    public static EntityType DEMON;
    public static void load() {
        PLAYER_SERVER = new EntityType(1, nidefawl.qubes.entity.PlayerServer.class, 0.8D, 0.8D, 1.6D);
        PLAYER = new EntityType(1, nidefawl.qubes.entity.PlayerRemote.class, 0.8D, 0.8D, 1.6D);
        CAT = new EntityType(2, nidefawl.qubes.entity.EntityCat.class, 0.5D, 0.6D, 0.8D);
        CHICKEN = new EntityType(3, nidefawl.qubes.entity.EntityChicken.class, 0.5D, 0.6D, 0.8D);
        DOG = new EntityType(4, nidefawl.qubes.entity.EntityDog.class, 0.5D, 0.6D, 0.8D);
        DUCK = new EntityType(5, nidefawl.qubes.entity.EntityDuck.class, 0.5D, 0.6D, 0.8D);
        GOAT = new EntityType(6, nidefawl.qubes.entity.EntityGoat.class, 0.5D, 0.6D, 0.8D);
        PIG = new EntityType(7, nidefawl.qubes.entity.EntityPig.class, 0.5D, 0.6D, 0.8D);
        PONY = new EntityType(8, nidefawl.qubes.entity.EntityPony.class, 0.5D, 0.6D, 0.8D);
        PUPPY = new EntityType(9, nidefawl.qubes.entity.EntityPuppy.class, 0.5D, 0.6D, 0.8D);
        SHEEP = new EntityType(10, nidefawl.qubes.entity.EntitySheep.class, 0.5D, 0.6D, 0.8D);
        SKELETON = new EntityType(11, nidefawl.qubes.entity.EntitySkeleton.class, 0.8D, 0.8D, 1.6D);
        ZOMBIE = new EntityType(12, nidefawl.qubes.entity.EntityZombie.class, 0.8D, 0.8D, 1.6D);
        ARCHER = new EntityType(13, nidefawl.qubes.entity.EntityArcher.class, 0.8D, 0.8D, 1.6D);
        WARRIOR = new EntityType(14, nidefawl.qubes.entity.EntityWarrior.class, 0.8D, 0.8D, 1.6D);
        DEMON = new EntityType(15, nidefawl.qubes.entity.EntityDemon.class, 0.8D, 0.8D, 1.6D);
    }
    /**
     * @param i
     * @param class1
     */
    public final int id;
    public final Class<? extends Entity> clazz;
    private double width;
    private double length;
    private double height;
    public EntityType(int id, Class<? extends Entity> clazz, double width, double length, double height) {
        mapping[id] = this;
        this.id = id;
        this.clazz = clazz;
        this.width = width;
        this.length = length;
        this.height = height;
    }

    
    public static Entity newById(int id, boolean serverEntity) {
        EntityType type = mapping[id];
        if (type == null) {
            throw new GameError("Invalid entity type id "+id);
        }
        return type.newInstance(serverEntity);
    }

    public Entity newInstance(boolean serverEntity) {
        try {
            return this.clazz.getDeclaredConstructor(boolean.class).newInstance(serverEntity);
        } catch (Exception e) {
            throw new GameError("Failed spawning entity", e);
        }
    }

    public double getWidth() {
        return this.width;
    }
    public double getHeight() {
        return this.height;
    }
    public double getLength() {
        return this.length;
    }


    public static boolean isValid(int t) {
        return t>1&&t<mapping.length&&mapping[t]!=null;
    }
}
