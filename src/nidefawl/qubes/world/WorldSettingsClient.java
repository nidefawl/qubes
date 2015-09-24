package nidefawl.qubes.world;

import java.util.UUID;

public class WorldSettingsClient implements IWorldSettings {
    UUID uuid;
    long seed;
    int time;
    private int id;
    private String worldName;
    public WorldSettingsClient(int id, UUID uuid, String name, long seed, int time) {
        this.id = id;
        this.uuid = uuid;
        this.worldName = name;
        this.seed = seed;
        this.time = time;
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public int getTime() {
        return this.time;
    }

    @Override
    public int getId() {
        return this.id;
    }
    
    @Override
    public String getName() {
        return this.worldName;
    }

}
