/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyframeTrans extends KeyFrame {
	/**
     * @param j
     * @param time
     * @param q
     */
    public QModelKeyframeTrans(int idx, float time, Vector3f q) {
        super(idx, time);
        this.param = q;
    }

    public Vector3f param;
    @Override
    public int getType() {
        return 1;
    }
}
