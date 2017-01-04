package nidefawl.qubes.world;

import java.util.ArrayList;
import java.util.HashMap;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class SunLightModel {

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
    long time = 15000;
    long dayLen = 30000;

    public SunLightModel() {
        this.sunModelView = new Matrix4f();
        this.moonModelView = new Matrix4f();
        this.sunPosition = new Vector3f();
        this.moonPosition = new Vector3f();
        this.lightPosition = new Vector3f();
        this.lightDirection = new Vector3f();
        this.tmp1 = new Vector3f();
    }

    float sunPathRotation = -15.0F;
    float moonPathRotation = -50.0F;

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
            float isnight = 0;
            if (sunPosition.y <= 0) {
                lightPosition.set(moonPosition);
                isnight=1;
            } else {
                lightPosition.set(sunPosition);
            }
            lightDirection.set(lightPosition);
            lightDirection.normalise();
            tmp1.set(0, 1, 0);
            lightAngleUp = Vector3f.dot(lightDirection, tmp1);
            dayLightIntensity = GameMath.clamp(lightAngleUp, 0.5f, 1.0f);
            dayNoon = (ca < 0.5 ? 1 - ca : ca)*2-1;
            nightNoon = GameMath.clamp((1-dayNoon)*120*isnight, 0, 1);
//            System.out.println(Math.max(1.0f-(lightAngleUp*lightAngleUp*2.0f), 0.0));
//            System.out.println((1.0f-((dayLightIntensity)-0.5f)*2.0f)*22);
        }
    }
    public void setDayLen(long dayLen) {
        this.dayLen = dayLen;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public float getSunAngle(float fTime) {
        //        if (time > 2000)
        //            time = 1200;
        //        if (time >1040) {
        //          fTime = 0;
        //        }
//                time = (int) (System.currentTimeMillis()/50L);
//                fTime = 0;
//        dayLen = 211500;
//        time = 138000;
////        time = 133000;
//        fTime=0;
//      dayLen = 4100;
//        if (this.settings.isFixedTime()) {
            fTime = 0;
//        }
        float timeOffset = (time) % dayLen;
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
