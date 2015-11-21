/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Matrix4f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyFrameMatrix extends KeyFrame {

    public Matrix4f mat;

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

}
