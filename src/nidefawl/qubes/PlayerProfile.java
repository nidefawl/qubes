package nidefawl.qubes;

import java.util.Random;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.config.InvalidConfigException;

public class PlayerProfile extends AbstractYMLConfig {
    /**
     * @param setDefaults
     */
    public PlayerProfile() {
        super(true);
    }

    public UUID uuid;
    private String name;

    @Override
    public void setDefaults() {
        Random rand = new Random();
        uuid = new UUID(rand.nextLong(), rand.nextLong());
        name = "Player";
    }

    public UUID getUUID() {
        return uuid;
    }


    /**
     * @param name2
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return the ingameName
     */
    public String getName() {
        return this.name;
    }

    @Override
    public void load() throws InvalidConfigException {
        this.uuid = UUID.fromString(getString("uuid", this.uuid.toString()));
        this.name = getString("name", this.name);
    }

    @Override
    public void save() {
        setString("uuid", this.uuid.toString());
        setString("name", this.name);
    }
}
