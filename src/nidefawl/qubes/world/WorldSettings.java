package nidefawl.qubes.world;

import java.io.*;
import java.util.Random;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorTest2;

public class WorldSettings extends AbstractYMLConfig implements IWorldSettings {

    public long  time = 78888;
    public long  dayLen = 120000;
    public boolean isFixedTime = true;
    public long seed;
    public UUID uuid;
    private File dir;
    private int id;
    public String generatorName;
    private String worldName;

    @Override
    public long getDayLen() {
        return dayLen;
    }

    @Override
    public boolean isFixedTime() {
        return isFixedTime;
    }
    @Override
    public void read(DataInput in) throws IOException {
        throw new UnsupportedOperationException("This method should not be called");
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeLong(this.uuid.getMostSignificantBits());
        out.writeLong(this.uuid.getLeastSignificantBits());
        out.writeLong(this.seed);
        out.writeLong(this.time);
        out.writeLong(this.dayLen);
        out.writeByte(this.isFixedTime ? 1 : 0);
        Packet.writeString(this.worldName, out);
        
    }

    public WorldSettings(File worldDirectory) {
        super(true);
        this.dir = worldDirectory;
        this.worldName = this.dir.getName();
    }

    @Override
    public void setDefaults() {
        this.seed = 0;
        this.time = 0;
        Random rand = new Random();
        this.uuid = new UUID(rand.nextLong(), rand.nextLong());
        this.generatorName = TerrainGeneratorTest2.GENERATOR_NAME;
    }

    @Override
    public void load() {
        String strseed = getString("seed", "0");
        this.seed = StringUtil.parseLong(strseed, 0, 16);
        this.generatorName = getString("generator", this.generatorName);
        this.time = getLong("time", this.time);
        this.dayLen = getLong("dayLength", this.dayLen);
        this.isFixedTime = getBoolean("isFixedTime", this.isFixedTime);
        String strUUID = getString("uuid", "");
        this.uuid = StringUtil.parseUUID(strUUID, this.uuid);
    }

    public File getWorldDirectory() {
        return this.dir;
    }

    @Override
    public void save() {
        setString("seed", Long.toHexString(this.seed));
        setString("generator", this.generatorName);
        setLong("time", this.time);
        setLong("dayLength", this.dayLen);
        setBoolean("isFixedTime", this.isFixedTime);
        setString("uuid", this.uuid.toString());
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
    public long getTime() {
        return this.time;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }


    /* (non-Javadoc)
     * @see nidefawl.qubes.world.IWorldSettings#getName()
     */
    @Override
    public String getName() {
        return this.worldName;
    }

    /**
     * 
     */
    public void saveFile() {
        try {
            write(new File(this.dir, "world.yml"));
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTime(long l) {
        this.time = l;
    }

    @Override
    public void setFixedTime(boolean b) {
        this.isFixedTime = b;
    }

    @Override
    public void setDayLen(long dayLen) {
        this.dayLen = dayLen;
    }
}
