package nidefawl.qubes.world;

import java.util.*;

import nidefawl.qubes.chunk.*;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator2;
import nidefawl.qubes.worldgen.TestTerrain2;

public abstract class World {
    public static final float MAX_XZ     = ChunkManager.MAX_CHUNK * Chunk.SIZE;
    public static final float MIN_XZ     = -MAX_XZ;
    HashMap<Integer, Entity>  entities   = new HashMap<>();                                             // use trove or something
    ArrayList<Entity>         entityList = new ArrayList<>();                                           // use fast array list
    public ArrayList<DynamicLight>         lights = new ArrayList<>();                                           // use fast array list

    public final int worldHeight;
    public final int worldHeightMinusOne;
    public final int worldHeightBits;
    public final int worldHeightBitsPlusFour;
    public final int worldSeaLevel;

    private long seed;

    public int         dayLen = 1000;
    public int         time;

    private final ChunkManager chunkMgr;
    private final Random       rand;
    private final UUID uuid;
    private int id;
    public static final int    MAX_WORLDHEIGHT = 256;

    public World(IWorldSettings settings) {
        this.id = settings.getId();
        this.chunkMgr = makeChunkManager();
        this.seed = settings.getSeed();
        this.uuid = settings.getUUID();
        this.time = settings.getTime();
        this.rand = new Random(seed);
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
//        this.generator = new TerrainGenerator2(this, this.seed);

    }

    public abstract ChunkManager makeChunkManager();

    public float getSunAngle(float fTime) {
        //        if (time > 2000)
        //            time = 1200;
        //        if (time >1040) {
        //          fTime = 0;
        //        }
//                time = (int) (System.currentTimeMillis()/50L);
//                fTime = 0;
        dayLen = 211500;
//        time = 53000;
        time = 163000;
        float timeOffset = (this.time) % dayLen;
        float fSun = (timeOffset + fTime) / (float) dayLen + 0.25F;
        if (fSun < 0)
            fSun++;
        if (fSun > 1)
            fSun--;
        float f = 1.0F - (float) (Math.cos(fSun * Math.PI) + 1) / 2.0F;
        return fSun + (f - fSun) / 3.0F;
//                return 0.88f;
        //        float a = timeOffset / (float) dayLen;
        ////        a = 0f;
        ////        a*=0.3f;
        //        return a;
        ////        return 0.8f+a;
    }
//    long lastCheck = System.currentTimeMillis();
//    int lastTime = this.time;
    public void tickUpdate() {
        this.time++;
        //        int offset = time%dayLen;
        //        if (offset < dayLen/3) {
        //            time += dayLen/3;
        //        }
        int size = this.entityList.size();
        for (int i = 0; i < size; i++) {
            Entity e = this.entityList.get(i);
            e.tickUpdate();
        }
//        long lPassed = System.currentTimeMillis()-lastCheck;
//        
//        if (lPassed >= 1000) {
//            float perSec = (this.time-this.lastTime) / (lPassed/1000.0F);
//            System.out.printf("%.2f ticks/s\n", perSec);
//            lastTime = time;
//            lastCheck = System.currentTimeMillis();
//        }
    }

    public int getType(int x, int y, int z) {
        if (y >= this.worldHeight)
            return 0;
        if (y < 0)
            return 0;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return 0;
        }
        return c.getTypeId(x & 0xF, y, z & 0xF);
    }
    
    public boolean setType(int x, int y, int z, int type, int render) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return false;
        }
        c.setType(x & 0xF, y, z & 0xF, type);
        if ((render & Flags.MARK) != 0) {
            flagBlock(x, y, z);
        }
        return true;
    }
    public abstract void flagBlock(int x, int y, int z);

    public Chunk getChunk(int x, int z) {
        return chunkMgr.get(x, z);
    }

    public void onLeave() {
        this.entities.clear();
        this.entityList.clear();
        this.getChunkManager().onWorldUnload();
    }


    public void onLoad() {
        this.getChunkManager().startThreads();
    }

    public void addEntity(Entity ent) {
        Entity e = this.entities.put(ent.id, ent);
        if (e != null) {
            throw new GameError("Entity with id " + ent.id + " already exists");
        }
        this.entityList.add(ent);
        ent.world = this;
        addLight(new Vector3f(ent.pos));
    }

    public void removeEntity(Entity ent) {
        Entity e = this.entities.remove(ent.id);
        if (e != null) {
            this.entityList.remove(e);
            ent.world = null;
        }
    }

    public void removeLight(int i) {
        if (this.lights.size() > 1) {
            this.lights.remove(this.lights.size()-1);
        }
    }

    public void addLight(Vector3f pos) {
        float r = this.rand.nextFloat()*0.5f+0.5f;
        float g = this.rand.nextFloat()*0.5f+0.5f;
        float b = this.rand.nextFloat()*0.5f+0.5f;
        float intens = 10+this.rand.nextInt(10);
//        if (this.rand.nextInt(10) == 0) {
            intens+=141;
//        }
        DynamicLight light = new DynamicLight(pos, new Vector3f(r, g, b),  intens);
        this.lights.add(light);
        System.out.println("added, size: "+lights.size());
    }

    public void spawnLights(BlockPos block) {
        for (int i = 0; i < 10; i++) {

            int range = 80;
            int x = block.x+this.rand.nextInt(range*2)-range;
            int z = block.z+this.rand.nextInt(range*2)-range;
            int y = getHighestBlockAt(x, z);
            addLight(new Vector3f(x+0.5F, y+1.2f+rand.nextFloat()*3.0f, z+0.5F));
        }
    }

    private int getHighestBlockAt(int x, int z) {
        Chunk c = this.getChunk(x>>Chunk.SIZE_BITS, z>>Chunk.SIZE_BITS);
        if (c != null) {
            return c.getTopBlock(x&Chunk.MASK, z&Chunk.MASK);
        }
        return 0;
    }
    public ChunkManager getChunkManager() {
        return this.chunkMgr;
    }


    public UUID getUUID() {
        return this.uuid;
    }

    public long getSeed() {
        return this.seed;
    }

    public int getTime() {
        return this.time;
    }

    /** dynamically generated at boot time 
     * do not use for storage
     * @return a consistent world-id at runtime only
     */
    public int getId() {
        return this.id;
    }
}
