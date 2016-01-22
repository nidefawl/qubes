package nidefawl.qubes.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.client.ChunkManagerClient;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.worldgen.biome.EmptyBiomeManager;
import nidefawl.qubes.worldgen.biome.HexBiomesClient;
import nidefawl.qubes.worldgen.biome.IBiomeManager;

public class WorldClient extends World {
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
    HashMap<Integer, Entity>  entities   = new HashMap<>();                                             // use trove or something
    ArrayList<Entity>         entityList = new ArrayList<>();                                           // use fast array list

    public WorldClient(WorldSettingsClient settings, int worldType) {
        super(settings);
        this.sunModelView = new Matrix4f();
        this.moonModelView = new Matrix4f();
        this.sunPosition = new Vector3f();
        this.moonPosition = new Vector3f();
        this.lightPosition = new Vector3f();
        this.lightDirection = new Vector3f();
        this.tmp1 = new Vector3f();
        this.biomeManager = createBiomeManager(worldType);
    }


    /**
     * @param worldType
     * @return
     */
    private IBiomeManager createBiomeManager(int worldType) {
        if (worldType == 0) {
            return new EmptyBiomeManager(this, this.settings.getSeed(), this.settings);
        }
        return new HexBiomesClient(this, this.settings.getSeed(), this.settings);
    }


    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerClient(this);
    }

    public void updateFrame(float fTime) {
        float sunPathRotation = -15.0F;
        float moonPathRotation = -50.0F;
        float ca = getSunAngle(fTime);
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
            float isNight = 0;
            if (sunPosition.y <= 0) {
                isNight = 1;
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
            nightNoon = GameMath.clamp(1-dayNoon+isNight*0.4f, 0, 1);
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

    public void flagBlock(int x, int y, int z) {
        Engine.regionRenderer.flagBlock(x, y, z);
    }
    
    @Override
    public void tickUpdate() {
        if (!this.settings.isFixedTime()) {
            this.settings.setTime(this.settings.getTime()+1L);
        }
        int size = this.entityList.size();
        for (int i = 0; i < size; i++) {
            Entity e = this.entityList.get(i);
            e.tickUpdate();
        }
        int size1 = this.lights.size();
        for (int i = 0; i < size1; i++) {
            DynamicLight e = this.lights.get(i);
            e.tickUpdate(this);
        }
    }



    public void onLeave() {
        this.entities.clear();
        this.entityList.clear();
    }

    public boolean addEntity(Entity ent) {
        Entity e = this.entities.put(ent.id, ent);
        if (e != null) {
            throw new GameError("Entity with id " + ent.id + " already exists: "+e);
        }
        this.entityList.add(ent);
        ent.world = this;
//        addLight(new Vector3f(ent.pos));
        return true;
    }

    public boolean removeEntity(Entity ent) {
        Entity e = this.entities.remove(ent.id);
        if (e != null) {
            this.entityList.remove(e);
            ent.world = null;
            return true;
        }
        return false;
    }


    public Entity getEntity(int entId) {
        return this.entities.get(entId);
    }

    public List<Entity> getEntityList() {
        return this.entityList;
    }

}
