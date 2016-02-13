/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelPoseBone {
    private final List<QModelPoseBone> children = Lists.newArrayList();
    public final Matrix4f matDeform = new Matrix4f();
    public final Matrix4f matDeformNormal = new Matrix4f();
    public final QModelBone restbone;
    public QModelPoseBone parent;
    public boolean animate = true;
    /**
     * @param b
     * @param parent2 
     */
    public QModelPoseBone(QModelBone b) {
        this.restbone = b;
        this.restbone.posebone = this;
        this.matDeform.load(b.matRest);
        updateNormalMat();
    }
    public void updateNormalMat() {
        this.matDeformNormal.load(this.matDeform);
        matDeformNormal.m30=0;
        matDeformNormal.m31=0;
        matDeformNormal.m32=0;
        matDeformNormal.m33=1;
        matDeformNormal.m03=0;
        matDeformNormal.m13=0;
        matDeformNormal.m23=0;
    }
    /**
     * @param jt
     */
    public void addChild(QModelPoseBone jt) {
        this.children.add(jt);
    }
    /**
     * @return the children
     */
    public List<QModelPoseBone> getChildren() {
        return this.children;
    }
    /**
     * @return
     */
    public boolean isDeform() {
        return this.restbone.isDeform();
    }
    /**
     * @return
     */
    public Matrix4f getMatRest() {
        return this.restbone.matRest;
    }
    /**
     * @return
     */
    public Matrix4f getMatDeform() {
        return this.matDeform;
    }
    /**
     * @return
     */
    public Vector3f getTailLocal() {
        return this.restbone.tailLocal;
    }
    /**
     * @return
     */
    public boolean isConnected() {
        return this.restbone.isConnected();
    }

    @Override
    public String toString() {
        return "QModelPoseBone"+this.restbone.stringInfo();
    }
}
