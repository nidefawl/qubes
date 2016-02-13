/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

import nidefawl.qubes.vec.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyFrameRotation extends KeyFrame {

    public final Quaternion q;

    /**
     * @param idx
     * @param time
     * @param mat 
     */
    public QModelKeyFrameRotation(int idx, float time, Quaternion q) {
        super(idx, time);
        this.q = q;
    }

    @Override
    public int getType() {
        return 2;
    }

}
