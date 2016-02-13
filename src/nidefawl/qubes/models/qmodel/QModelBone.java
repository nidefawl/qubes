/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelBone {
    
	public int idx;
	public int flags;
    public String name;
    public String parentName;
    public Matrix4f matRest;
    public Matrix4f matRestInv = new Matrix4f();
    private final List<QModelBone> children = Lists.newArrayList();
    public Vector3f tailLocal = new Vector3f();
    public QModelBone parent;
    public QModelPoseBone posebone;
    public float boneLength;
    /**
     * @param i 
     * @param modelLoaderQModel
     * @throws EOFException 
     */
    public QModelBone(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUByte();
        this.name = loader.readString(32);
        this.parentName = loader.readString(32);
        float[] mat = new float[16];
        this.matRest = new Matrix4f();
        for (int j = 0; j < 16; j++) {
            mat[j] = loader.readFloat();
        }
        this.matRest.load(mat);
        Matrix4f.invert(this.matRest, this.matRestInv);
        Vector3f vec = loader.readVec3();
        Matrix4f.transform(this.matRestInv, vec, this.tailLocal);
        this.boneLength = this.tailLocal.length();
    }
    /**
     * @param jt
     */
    public void addChild(QModelBone jt) {
        this.children.add(jt);
    }
    /**
     * @return the children
     */
    public List<QModelBone> getChildren() {
        return this.children;
    }
    /**
     * @return
     */
    public boolean isDeform() {
        return (this.flags&2)!=0;
    }
    /**
     * @return
     */
    public boolean isConnected() {
        return (this.flags&1)==0;
    }
    
    public String stringInfo() {
        return "[name="+name+",parent="+parent+",idx="+idx+",children-len:"+children.size()+"]";
    }
    @Override
    public String toString() {
        return "QModelBone"+stringInfo();
    }

}
