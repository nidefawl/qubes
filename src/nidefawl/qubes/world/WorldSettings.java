package nidefawl.qubes.world;

import java.io.*;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;

public class WorldSettings extends AbstractYMLConfig implements IWorldSettings {

    public long  time;
    public long  dayLen = 120000;
    public boolean isFixedTime;
    public long seed;
    public UUID uuid;
    private final File dir;
    private int id;
    private int generator;
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
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void load() {
        String strseed = getString("seed", "0");
        this.seed = StringUtil.parseLong(strseed, 0, 16);
        this.generator = getInt("generator", this.generator);
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
        setInt("generator", this.generator);
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

    /**
     * @return
     */
    public ITerrainGen getGenerator(WorldServer w) {
        switch (this.generator) {
            case 0:
                return new TestTerrain2(w, this.getSeed());
            case 1:
                return new TerrainGenerator2(w, this.getSeed());
            case 2:
                return new TestTerrain(w, this.getSeed());
            case 3:
                return new TerrainGenerator(w, this.getSeed());
        }
        return new TestTerrain2(w, this.getSeed());
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.world.IWorldSettings#getName()
     */
    @Override
    public String getName() {
        return this.worldName;
    }

    /**
     * @param worldServer
     * @return
     */
    public IChunkPopulator getPopulator(WorldServer worldServer) {
        switch (this.generator) {
            case 0:
            case 2:
                return new EmptyChunkPopulator();
        }
        return new ChunkPopulator();
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
