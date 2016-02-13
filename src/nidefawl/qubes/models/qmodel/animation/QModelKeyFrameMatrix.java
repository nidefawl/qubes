/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

import nidefawl.qubes.vec.Matrix4f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyFrameMatrix extends KeyFrame {

    public final Matrix4f mat;

    /**
     * @param idx
     * @param time
     * @param mat 
     */
    public QModelKeyFrameMatrix(int idx, float time, Matrix4f mat) {
        super(idx, time);
        this.mat = mat;
    }

    @Override
    public int getType() {
        return 0;
    }

    public KeyFrame copy() {
        QModelKeyFrameMatrix mat = new QModelKeyFrameMatrix(this.idx, this.time, this.mat);
        return mat;
    }
}
