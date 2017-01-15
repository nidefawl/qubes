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
    public final Vector3f t       = new Vector3f();
    Matrix4f matTmp1;
    Matrix4f matTmp2;

    public void updateMouseFromScreenPos(float winX, float winY, float renderWidth, float renderHeight, Vector3f cameraOffset) {
        Engine.unprojectScreenSpace(winX, winY, 1, renderWidth, renderHeight, vTarget);
        vTarget.add(Engine.GLOBAL_OFFSET);
        Engine.unprojectScreenSpace(winX, winY, 0, renderWidth, renderHeight, vOrigin);
        vOrigin.add(Engine.GLOBAL_OFFSET);
        Vector3f.sub(vTarget, vOrigin, vDirTmp);
        vDir = vDirTmp.normaliseNull();
        if (vDir != null) {
            if (cameraOffset != null) {
                vOrigin.subtract(cameraOffset);
                vDir.subtract(cameraOffset);
            }
            t.set(vDir);
            t.scale(-0.1F);
            Vector3f.add(vOrigin, t, vOrigin);
        }
    }

    public void updateFromController(Matrix4f matrix4f) {
        Matrix4f m = matTmp1;
        m.load(matrix4f);
        m.rotate(-33 * GameMath.PI_OVER_180, 1, 0, 0);
        t.set(0, 0, 0);
        Matrix4f.transform(m, t, vOrigin);
        t.set(0, 0, -10);
        Matrix4f.transform(m, t, t);
        t.subtract(vOrigin);
        vOrigin.addVec(Engine.camera.getPosition());
        vDir = t.normaliseNull();
        if (vDir != null) {
//            if (cameraOffset != null) {
//                vOrigin.subtract(cameraOffset);
//                vDir.subtract(cameraOffset);
//            }
            t.set(vDir);
            t.scale(-0.1F);
            Vector3f.add(vOrigin, t, vOrigin);
        }
    }

    public void reset() {
        this.vDir = null;
    }
}
