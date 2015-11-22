/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.List;

import com.google.common.collect.Lists;

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
    QBoneAnimation animation;
    private final List<QModelBone> children = Lists.newArrayList();
    public Vector3f tail;
    public Vector3f tailLocal = new Vector3f();
    public QModelBone parent;
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
        this.tail = loader.readVec3();
        int numFrames = loader.readUShort();
        this.animation = new QBoneAnimation(numFrames);
        for (int j = 0; j < numFrames; j++) {
            float time = loader.readFloat();
            Matrix4f matAnim = new Matrix4f();
            for (int k = 0; k < 16; k++) {
                mat[k] = loader.readFloat();
            }
            matAnim.load(mat);
            QModelKeyFrameMatrix frame = new QModelKeyFrameMatrix(j, time, matAnim);
            this.animation.addFrame(frame);
        }
        Matrix4f.invert(this.matRest, this.matRestInv);
        Matrix4f.transform(this.matRestInv, this.tail, this.tailLocal);
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
