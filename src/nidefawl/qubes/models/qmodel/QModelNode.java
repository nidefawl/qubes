package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.Matrix4f;

public class QModelNode extends QModelAbstractNode {
    public final int idx;
    public final String name;
    public final String parent_name;
    public final int parent_type;
    private QModelBone attachmentBone = null;
    private QModelAbstractNode attachmentEmpty;
    public Matrix4f matDeform = new Matrix4f();
    public final Matrix4f matDeformNormal = new Matrix4f();
    public final Matrix4f localMat;

    public QModelNode(int idx, ModelLoaderQModel loader) throws EOFException {
        this.idx = idx;
        this.name = loader.readString(32);
        this.parent_name = loader.readString(32);
        this.parent_type = readParentType(loader);
        this.localMat = new Matrix4f();
        float[] mat = new float[16];
        for (int l = 0; l < 16; l++) {
            mat[l] = loader.readFloat();
        }
        this.localMat.load(mat);

        updateNormalMat();
    }


    @Override
    public String toString() {
        return "ModelNode["+idx+", "+name+"]";
    }


    public void resolveParent(ModelLoaderQModel loader) {
        if (this.parent_type == 2) {
            this.attachmentBone = loader.findJoint(this.parent_name);
        }
        if (this.parent_type == 3) {
            this.attachmentEmpty = loader.findEmpty(this.parent_name);
        }
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


    @Override
    public QModelAbstractNode getAttachementNode() {
        return this.attachmentEmpty;
    }


    @Override
    public QModelBone getAttachmentBone() {
        return this.attachmentBone;
    }


    @Override
    public Matrix4f getMatDeform() {
        return this.matDeform;
    }


    @Override
    public Matrix4f getMatDeformNormal() {
        return this.matDeformNormal;
    }
}
