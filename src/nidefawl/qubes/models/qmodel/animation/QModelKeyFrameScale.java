/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyFrameScale extends KeyFrame {

    public final Vector3f scale;

    /**
     * @param idx
     * @param time
     * @param mat 
     */
    public QModelKeyFrameScale(int idx, float time, Vector3f rot) {
        super(idx, time);
        this.scale = rot;
    }

    @Override
    public int getType() {
        return 3;
    }

}
