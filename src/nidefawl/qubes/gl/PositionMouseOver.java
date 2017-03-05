package nidefawl.qubes.gl;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vr.VR;

public class PositionMouseOver {

    public Vector3f       vDir    = null;
    public final Vector3f vOrigin = new Vector3f();
    public final Vector3f vDirTmp = new Vector3f();
    public final Vector3f vTarget = new Vector3f();
     final Vector3f t       = new Vector3f();

    public void updateMouseFromScreenPos(float winX, float winY, float windowWidth, float windowHeight, Vector3f cameraOffset) {
        Vector3f rayDirWorldSpace = Vector3f.pool();
        Engine.unprojectScreenSpace(winX, winY, 1, windowWidth, windowHeight, rayDirWorldSpace);
        Engine.unprojectScreenSpace(winX, winY, 0, windowWidth, windowHeight, vOrigin);
//        System.out.println(vOrigin);
        rayDirWorldSpace.subtract(vOrigin);
        float length = rayDirWorldSpace.length();
        if (length > 1E-8F) {
            rayDirWorldSpace.scale(1.0F/length);
            t.set(rayDirWorldSpace);
            Vector3f.add(vOrigin, Vector3f.pool(rayDirWorldSpace).scale(0.1F), vOrigin);
            vDir = t;
//            System.out.println("t "+t);
        } else {
            vDir = null;
        }
        if (cameraOffset != null) {
            vOrigin.subtract(cameraOffset);
        }
    }

    public void updateFromController(int idx, float f) {
        Matrix4f poseInverse = Matrix4f.pool();
        Matrix4f.mul(VR.offsetPoseInv, VR.poseMatrices[idx], poseInverse);
        poseInverse.rotate(-33 * GameMath.PI_OVER_180, 1, 0, 0);
        Matrix4f.transform(poseInverse, Vector3f.ZERO, vOrigin);
        Vector3f rayDirWorldSpace = Vector3f.pool(0.f, 0.f, -10.f);
        Matrix4f.transform(poseInverse, rayDirWorldSpace, rayDirWorldSpace);
        rayDirWorldSpace.subtract(vOrigin);
        vOrigin.addVec(Engine.camera.getPosition());
        float length = rayDirWorldSpace.length();
        if (length > 1E-8F) {
            rayDirWorldSpace.scale(1.0F/length);
            t.set(rayDirWorldSpace);
            Vector3f.add(vOrigin, Vector3f.pool(rayDirWorldSpace).scale(-0.1F), vOrigin);
            vDir = t;
        } else {
            vDir = null;
        }
//        if (cameraOffset != null) {
//            vOrigin.subtract(cameraOffset);
//        }
    }

    public void reset() {
        this.vDir = null;
    }
}
