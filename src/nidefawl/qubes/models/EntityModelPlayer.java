package nidefawl.qubes.models;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelRigged;
import nidefawl.qubes.render.BatchedRiggedModelRenderer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelPlayer extends EntityModel {

    final Matrix4f modelMat = new Matrix4f();
    final Matrix4f normalMat = new Matrix4f();
    public EntityModelPlayer(String name) {
        super(name);
    }
    
    @Override
    public void setPose(BatchedRiggedModelRenderer render, Entity e, float fabs, float fTime, Vector3f rot, Vector3f pos) {
        FloatBuffer bufferModel = render.getBufModelMat();
        FloatBuffer bufferNormal = render.getBufNormalMat();
        FloatBuffer bufferBones = render.getBufBoneMat();
        float headYaw = rot.x;
        float yaw = rot.y;
        float pitch = rot.z;
        yaw -= headYaw;
        float rotYBase = -1 * yaw - 90;
        float modelcale = 1 / 3.7f;
        Matrix4f mat = this.modelMat;
        mat.setIdentity();
        mat.translate(pos.x, pos.y, pos.z);
        mat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
        mat.rotate(rotYBase * GameMath.PI_OVER_180, 0, 1, 0);
        mat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
        mat.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
        mat.scale(modelcale);
        mat.store(bufferModel);
        mat = this.normalMat;
        mat.setIdentity();
        mat.rotate(rotYBase * GameMath.PI_OVER_180, 0, 1, 0);
        mat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
        mat.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
        mat.invert().transpose();
        mat.store(bufferNormal);
        ModelRigged model = (ModelRigged) this.model;
        
        
        model.setAction(0);
        int type = 0;
        if (e instanceof Player && ((Player) e).punchTicks > 0) {
            int maxTicks = 8;
            fabs = (maxTicks - (((Player) e).punchTicks - fTime)) / ((float) maxTicks - 1);
            model.setAction(1);
            type = 1;
        } else if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
            model.setAction(2);
            fabs *= 8;
            fabs %= 4f;
            fabs /= 4f;
            type = 1;
        }

        model.animate(type, fabs);
        model.setHeadOrientation(270 + headYaw, pitch);
        int n = model.storeMatrices(bufferBones);
//        System.out.println(""+n+" bones");
        render.submitModel();
    }
    
    public Matrix4f getNormalMat() {
        return this.normalMat;
    }
    public Matrix4f getModelMat() {
        return this.modelMat;
    }

}
