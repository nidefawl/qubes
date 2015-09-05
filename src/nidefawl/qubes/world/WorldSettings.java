package nidefawl.qubes.world;

import java.io.File;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.util.StringUtil;

public class WorldSettings extends AbstractYMLConfig implements IWorldSettings {

    public int  time;
    public long seed;
    public UUID uuid;
    private final File dir;
    private int id;

    public WorldSettings(File worldDirectory) {
        super(true);
        this.dir = worldDirectory;
    }

    @Override
    public void setDefaults() {
        this.seed = 0;
        this.time = 0;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void load() {
        String strseed = getString("seed", "0");
        this.seed = StringUtil.parseLong(strseed, 0);
        this.time = getInt("time", this.time);
        String strUUID = getString("uuid", "");
        this.uuid = StringUtil.parseUUID(strUUID, this.uuid);
    }

    public File getWorldDirectory() {
        return this.dir;
    }

    @Override
    public void save() {
        setString("uuid", this.uuid.toString());
        setString("seed", Long.toHexString(this.seed));
        setInt("time", this.time);
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

    public void setId(int id) {
        this.id = id;
    }
}
