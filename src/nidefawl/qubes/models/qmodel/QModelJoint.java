/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Quaternion;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelJoint {

	public int idx;
	public int flags;
	public String name;
	public String parentName;
    public Matrix4f matRest;
    public Matrix4f matDeform;
    QJointAnimation animation;
	public Vector3f color = Vector3f.ONE;
    private List<QModelJoint> children = Lists.newArrayList();
    public Vector3f tail;
    public Vector3f tailLocal = new Vector3f();
    public Matrix4f matRestInv = new Matrix4f();
    public QModelJoint parent;
    /**
     * @param jt
     */
    public void addChild(QModelJoint jt) {
        this.children.add(jt);
    }
    /**
     * @return the children
     */
    public List<QModelJoint> getChildren() {
        return this.children;
    }

}
