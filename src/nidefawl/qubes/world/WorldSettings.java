package nidefawl.qubes.world;

import java.io.File;
import java.util.UUID;

import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.worldgen.*;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;

public class WorldSettings extends AbstractYMLConfig implements IWorldSettings {

    public int  time;
    public long seed;
    public UUID uuid;
    private final File dir;
    private int id;
    private int generator;
    private String worldName;

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
        this.seed = StringUtil.parseLong(strseed, 0);
        this.time = getInt("time", this.time);
        this.generator = getInt("generator", this.generator);
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
        setInt("generator", this.generator);
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
}
