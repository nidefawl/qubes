package nidefawl.qubes.particle;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class CubeParticle extends Particle {
    public int tex;
    public int normalMap;
    public int type = 1;
    public int pass;
    public Vector3f color = new Vector3f();

    public CubeParticle() {
        super();
//        noclip=true;
    }
    
    public void setTex(int tex) {
        this.tex = tex;
    }
    public void setType(int type) {
        this.type = type;
    }


    public int store(int offset, FloatBuffer bufMatFloat, IntBuffer bufBlockInfo) {
        BufferedMatrix mat = Engine.getTempMatrix();
        mat.setIdentity();
        mat.translate(this.renderPos);
        mat.rotate(this.renderRot.y*GameMath.PI*2.0f, 0.0f, 1.0f, 0.0f);
        mat.rotate(this.renderRot.x*GameMath.PI*2.0f, 1.0f, 0.0f, 0.0f);
        mat.rotate(this.renderRot.z*GameMath.PI*2.0f, 0.0f, 0.0f, 1.0f);
        mat.scale(this.renderSize);
//        System.out.println(this.renderSize);
        mat.store(bufMatFloat);
        int attr = this.tex | this.normalMap << 12 | this.type << 16 | this.pass << (16+12);
        bufBlockInfo.put(Float.floatToIntBits(this.color.x));
        bufBlockInfo.put(Float.floatToIntBits(this.color.y));
        bufBlockInfo.put(Float.floatToIntBits(this.color.z));
        bufBlockInfo.put(Float.floatToIntBits(this.initSize));
        bufBlockInfo.put(attr);
        bufBlockInfo.put(this.lightValue);
        bufBlockInfo.put(Float.floatToRawIntBits(this.texOffset.x));
        bufBlockInfo.put(Float.floatToRawIntBits(this.texOffset.y));
        return 1;
    }


    public void tickUpdate(World world) {
        super.tickUpdate(world);
        if (!sleeping)
        rot.addVec(this.rotspeed);
        float fScale = 1.0f;
        if (tick > maxLive*4/5) {
            int iTick = tick - (maxLive*4/5);
            fScale = iTick / (float)(maxLive/5);
            fScale = 1.0f-fScale;
            fScale*=fScale;
            size = initSize*fScale;
        }
        if (hitGround) {
            rotspeed.scale(mot.length()*3);
        } else {
//            System.out.println("midair");
        }
//        rotspeed.scale(0.98f);
//        mot.x *= 0.98f;
//        mot.y *= 0.98f;
//        mot.z *= 0.98f;
//        mot.y -= 0.01f;
        tick++;
        if (tick > maxLive) {
            die();
        }
    }


    public void setTextureOffset(float u, float v) {
        this.texOffset.set(u, v);
    }
}