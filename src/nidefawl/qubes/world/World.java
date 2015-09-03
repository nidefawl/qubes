package nidefawl.qubes.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import nidefawl.qubes.Main;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator2;
import nidefawl.qubes.worldgen.TestTerrain2;

public class World {
    public static final float MAX_XZ     = RegionLoader.MAX_REGION_XZ * Region.REGION_SIZE * Chunk.SIZE;
    public static final float MIN_XZ     = -MAX_XZ;
    HashMap<Integer, Entity>  entities   = new HashMap<>();                                             // use trove or something
    ArrayList<Entity>         entityList = new ArrayList<>();                                           // use fast array list
    public ArrayList<Light>         lights = new ArrayList<>();                                           // use fast array list

    public final int worldHeight;
    public final int worldHeightMinusOne;
    public final int worldHeightBits;
    public final int worldHeightBitsPlusFour;
    public final int worldSeaLevel;

    private long seed;

    private AbstractGen generator;
    public int          worldId;
    private int         dayLen = 1000;
    private int         time;

    private final RegionLoader regionLoader;
    private final Random       rand;
    public static final int    MAX_WORLDHEIGHT = 256;

    float dayLightIntensity;
    float nightNoon;
    float dayNoon;
    float lightAngleUp;
    /** private scratchpad fields **/
    private final Matrix4f       sunModelView;
    private final Matrix4f       moonModelView;
    private final Vector3f       sunPosition;
    private final Vector3f       moonPosition;
    private final Vector3f       lightPosition;
    private final Vector3f       lightDirection;
    private final Vector3f       tmp1;
    
    public World(int worldId, long seed, RegionLoader regionLoader) {
        this.regionLoader = regionLoader;
        this.seed = seed;
        this.rand = new Random(seed);
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
        this.generator = new TestTerrain2(this, this.seed);
        this.sunModelView = new Matrix4f();
        this.moonModelView = new Matrix4f();
        this.sunPosition = new Vector3f();
        this.moonPosition = new Vector3f();
        this.lightPosition = new Vector3f();
        this.lightDirection = new Vector3f();
        this.tmp1 = new Vector3f();

    }

    public Chunk generateChunk(int i, int j) {
        return this.generator.generateChunk(i, j);
    }

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

    public void tickUpdate() {
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
            throw new GameError("Entity with id " + ent.id + " already exists");
        }
        this.entityList.add(ent);
        ent.world = this;
        addLight(new Vector3f(ent.pos));
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
        Light light = new Light(pos, new Vector3f(r, g, b),  intens);
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

    public void updateFrame(float fTime) {
        float sunPathRotation = -15.0F;
        float moonPathRotation = -50.0F;
        float ca = this.getSunAngle(fTime);
        {
            float angle = ca * 360.0F;
            sunModelView.setIdentity();
            sunModelView.rotate(-90.0F * GameMath.PI_OVER_180, 0f, 1f, 0f);
            sunModelView.rotate(sunPathRotation * GameMath.PI_OVER_180, 0.0F, 0.0F, 1.0F);
            sunModelView.rotate(angle * GameMath.PI_OVER_180, 1.0F, 0.0F, 0.0F);
            sunPosition.set(0, 100, 0);
            Matrix4f.transform(sunModelView, sunPosition, sunPosition);
            moonModelView.setIdentity();
            moonModelView.rotate(-90.0F * GameMath.PI_OVER_180, 0f, 1f, 0f);
            moonModelView.rotate(angle * GameMath.PI_OVER_180, 0f, 0f, 1f);
            moonModelView.rotate(moonPathRotation * GameMath.PI_OVER_180, 1f, 0f, 0f);
            moonPosition.set(0, -100, 0);
            Matrix4f.transform(moonModelView, moonPosition, moonPosition);
            if (sunPosition.y <= 0) {
                lightPosition.set(moonPosition);
            } else {
                lightPosition.set(sunPosition);
            }
            lightDirection.set(lightPosition);
            lightDirection.normalise();
            tmp1.set(0, 3, 0);
            lightAngleUp = Vector3f.dot(lightDirection, tmp1);
            dayLightIntensity = GameMath.clamp(lightAngleUp, 0.5f, 1.0f);
            dayNoon = (ca < 0.5 ? 1 - ca : ca)*2-1;
            nightNoon = 1-dayNoon;
        }
    }
    public Vector3f getLightPosition() {
        return lightPosition;
    }
    public float getLightAngleUp() {
        return lightAngleUp;
    }
    
    public float getDayLightIntensity() {
        return dayLightIntensity;
    }
    
    public float getDayNoonFloat() {
        return dayNoon;
    }
    
    public float getNightNoonFloat() {
        return nightNoon;
    }

}
