package nidefawl.qubes.world;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.client.ChunkManagerClient;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

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

    public WorldClient(WorldSettings settings) {
        super(settings);
        this.sunModelView = new Matrix4f();
        this.moonModelView = new Matrix4f();
        this.sunPosition = new Vector3f();
        this.moonPosition = new Vector3f();
        this.lightPosition = new Vector3f();
        this.lightDirection = new Vector3f();
        this.tmp1 = new Vector3f();
    }


    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerClient(this);
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
