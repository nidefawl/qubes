package nidefawl.qubes.gl;

import nidefawl.qubes.vec.Vector3f;

public class PositionMouseOver {

    public Vector3f       vDir    = null;
    public final Vector3f vOrigin = new Vector3f();
    public final Vector3f vDirTmp = new Vector3f();
    public final Vector3f vTarget = new Vector3f();
    public final Vector3f t       = new Vector3f();

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
}
