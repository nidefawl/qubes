/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyFramePosition extends KeyFrame {

    public final Vector3f pos;

    /**
     * @param idx
     * @param time
     * @param mat 
     */
    public QModelKeyFramePosition(int idx, float time, Vector3f pos) {
        super(idx, time);
        this.pos = pos;
    }

    @Override
    public int getType() {
        return 1;
    }

}
