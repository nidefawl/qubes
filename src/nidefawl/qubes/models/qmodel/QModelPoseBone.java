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
    public Matrix4f matDeform;
    public final Matrix4f deformInterp = new Matrix4f();
    public final QModelBone restbone;
    public QModelPoseBone parent;
    /**
     * @param b
     * @param parent2 
     */
    public QModelPoseBone(QModelBone b) {
        this.restbone = b;
        this.matDeform = deformInterp;
        this.matDeform.load(b.matRest);
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
     * @param mat
     * @param mat2
     * @param frameInterpProgress
     */
    public void interpolateFrame(Matrix4f mat, Matrix4f mat2, float frameInterpProgress) {
        deformInterp.setZero();
        deformInterp.addWeighted(mat, 1.0f-frameInterpProgress);
        deformInterp.addWeighted(mat2, frameInterpProgress);
        this.matDeform = deformInterp;
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
    public QBoneAnimation getAnimation() {
        return this.restbone.animation;
    }
}
