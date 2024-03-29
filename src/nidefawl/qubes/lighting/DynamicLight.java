package nidefawl.qubes.lighting;

import java.nio.FloatBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;

public class DynamicLight {
    public Vector3f pos = new Vector3f();
    public Vector3f lastPos = new Vector3f();
    public Vector3f mot = new Vector3f();
    public Vector3f lastMot = new Vector3f();
    public Vector3f renderPos = new Vector3f();
    public Vector3f color;
    public float intensity;
    public float quadratic =1f;
    public float lightThreshold = 0.1f;
    public float radius;
    public int ticks;
    public DynamicLight(Vector3f loc, Vector3f color, float intensity) {
        setPos(loc);
        this.color = color;
        this.intensity = intensity;
        this.radius = GameMath.sqrtf(1.0f / (quadratic * 0.01f));
    }
    private void setPos(Vector3f loc) {
        this.pos.set(loc);
        this.lastPos.set(this.pos);
    }
    public void tickUpdate(WorldClient w) {
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        ticks++;
//        this.mot.y = y;
//        this.pos.addVec(this.mot);
    }

    public void updatePreRender(WorldClient w, float f) {
        Vector3f.interp(this.lastPos, this.pos, f, this.renderPos);
        this.renderPos.x-=Engine.GLOBAL_OFFSET.x;
        this.renderPos.y-=Engine.GLOBAL_OFFSET.y;
        this.renderPos.z-=Engine.GLOBAL_OFFSET.z;
        float rTick = (ticks+((this.hashCode()*219)%40000)+f)*0.01f;
        float ff = rTick*GameMath.PI*2.0f;
        float sin = GameMath.sin(ff%(GameMath.PI*2.0f));
        float y = (1+sin)*1.56f;
        this.renderPos.y+=0;
//        this.intensity = 0.6f;//1.5f+(sin*0.5f+0.5f)*2f;
        this.quadratic = 1F/this.intensity;
        this.radius = GameMath.sqrtf(1.0f / (quadratic * 0.01f));
//        System.out.println(this.radius);
//      System.out.println(renderPos);
//        System.out.println(radius);
    }
    public void store(FloatBuffer lightBuf) {
//        System.out.println(radius);
        this.renderPos.store(lightBuf);
        lightBuf.put(0);
        this.color.store(lightBuf);
        lightBuf.put(0);
        lightBuf.put(intensity); 
        lightBuf.put(radius);
        lightBuf.put(quadratic);
//        System.out.println(Engine.GLOBAL_OFFSET);
    }
}
