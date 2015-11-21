/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Quaternion;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelKeyframeRot extends KeyFrame {
    public QModelKeyframeRot(int idx, float time, Quaternion q) {
        super(idx, time);
        this.param = q;
    }
	public Quaternion param;
	@Override
	public int getType() {
	    return 0;
	}
}
