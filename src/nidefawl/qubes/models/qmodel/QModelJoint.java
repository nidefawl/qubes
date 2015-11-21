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
	public Quaternion rotation;
	public Vector3f position;
	QJointAnimation animation;
	public Vector3f color = Vector3f.ONE;
    public QModelJoint parent;
    public Matrix4f matLocal;
    public Matrix4f matAbs;
    public Matrix4f matFinal;
    public Matrix4f matFinal2;
    private List<QModelJoint> children = Lists.newArrayList();
    public Matrix4f modelMat;
    public Vector3f direction;
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
