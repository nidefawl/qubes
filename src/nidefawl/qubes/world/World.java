package nidefawl.qubes.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator;
import nidefawl.qubes.worldgen.TerrainGenerator2;

public class World {
    public static final float MAX_XZ = RegionLoader.MAX_REGION_XZ*Region.REGION_SIZE*Chunk.SIZE;
    public static final float MIN_XZ = -MAX_XZ;
    HashMap<Integer, Entity>      entities = new HashMap<>(); // use trove or something
    ArrayList<Entity>      entityList = new ArrayList<>(); // use fast array list

    public final int     worldHeight;
    public final int     worldHeightMinusOne;
    public final int     worldHeightBits;
    public final int     worldHeightBitsPlusFour;
    public final int     worldSeaLevel;

    private long         seed;

    private AbstractGen generator;
    public int worldId;
    private int dayLen=1000;
    private int time;

    private final RegionLoader regionLoader;
    public static final int MAX_WORLDHEIGHT = 256;

    public World(int worldId, long seed, RegionLoader regionLoader) {
        this.regionLoader = regionLoader;
        this.seed = seed;
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
        this.generator = new TerrainGenerator2(this, this.seed);
        
    }

    public Chunk generateChunk(int i, int j) {
        return this.generator.generateChunk(i, j);
    }
    public float getSunAngle(float fTime) {
        int timeOffset = this.time%dayLen;
        float fSun = (timeOffset+fTime)/(float) dayLen + 0.25F;
        if (fSun<0)fSun++;
        if (fSun>1)fSun--;
        float f = 1.0F - (float)(Math.cos(fSun*Math.PI)+1)/2.0F;
//        return fSun+(f-fSun)/3.0F;
        return 0.78F;
    }

    public void tickUpdate() {
        dayLen = 7000;
        this.time++;
//        int offset = time%dayLen;
//        if (offset < dayLen/3) {
//            time += dayLen/3;
//        }
        int size = this.entities.size();
        for (int i = 0; i < size; i++) {
            Entity e = this.entities.get(i);
            e.tickUpdate();
        }
    }
    public int getType(int x, int y, int z) {
        if (y >= this.worldHeight)
            return 0;
        if (y < 0)
            return 0;
        Chunk c = getChunk(x>>4, z>>4);
        if (c == null) {
            return 0;
        }
        return c.getTypeId(x&0xF, y, z&0xF);
    }
    public boolean setType(int x, int y, int z, int type, int render) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x>>4, z>>4);
        if (c == null) {
            return false;
        }
        c.setType(x&0xF, y, z&0xF, type);
        if ((render & Flags.RENDER) != 0) {
            Engine.regionRenderer.flagBlock(x, y, z);
        }
        return true;
    }
    
    public Chunk getChunk(int x, int z) {
        return regionLoader.get(x, z);
    }

    public void onLeave() {
        this.entities.clear();
        this.entityList.clear();
    }

    public void addEntity(Entity ent) {
        Entity e = this.entities.put(ent.id, ent);
        if (e != null) {
            throw new GameError("Entity with id "+ent.id+" already exists");
        }
        this.entityList.add(ent);
        ent.world = this;
    }
    
    
}
