package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.Matrix4f;

public class QModelObject extends QModelAbstractNode {
    public final int idx;
    public int type;
    public final String name;
    public final String parent_name;
    public final int parent_type;
    private QModelBone attachmentBone = null;
    private QModelAbstractNode attachmentEmpty;
    public Matrix4f matDeform = new Matrix4f();
    public final Matrix4f matDeformNormal = new Matrix4f();
    
    public List<QModelVertex> listVertex;
    public List<QModelTriangle> listTri;
    public List<QModelGroup> listGroups;
    public List<QModelBone> listBones;
    public boolean isSkinned;
    
    /**
     * @param idx
     * @param modelLoaderQModel
     * @throws EOFException 
     */
    public QModelObject(int idx, ModelLoaderQModel loader) throws EOFException {
        this.idx = idx;
        this.type = loader.readUByte();
        this.name = loader.readString(32);
        this.parent_name = loader.readString(32);
        this.parent_type = readParentType(loader);
        this.isSkinned = type == 1;
        int numVertices = loader.readUShort();
        listVertex = Lists.newArrayListWithCapacity(numVertices);
        for (int i = 0; i < numVertices; i++) {
            QModelVertex v = new QModelVertex(i, loader);
            listVertex.add(v);
        }
        int numTriangles = loader.readUShort();
        listTri = Lists.newArrayListWithCapacity(numTriangles);
        for (int i = 0; i < numTriangles; i++) {
            QModelTriangle tri = new QModelTriangle(i, loader);
            listTri.add(tri);
        }
        int numGroups = loader.readUShort();
        listGroups = Lists.newArrayListWithCapacity(numGroups);
        for (int i = 0; i < numGroups; i++) {
            QModelGroup group = new QModelGroup(i, loader);
            group.isSkinned = this.isSkinned;
            listGroups.add(group);
        }
        for (int i = 0; i < numGroups; i++) {
            QModelGroup group = this.listGroups.get(i);
            group.listTri = Lists.newArrayList();
            for (int j = 0; j < group.triIdx.length; j++) {
                int tidx = group.triIdx[j];
                group.listTri.add(listTri.get(tidx));
            }
        }
        updateNormalMat();
    }


    @Override
    public String toString() {
        return "ModelObject["+idx+", "+name+"]";
    }
    public void flattenBoneList(ModelLoaderQModel loader) {

        if (this.isSkinned) {
            ArrayList<Integer> refBones = new ArrayList<>();
            for (int i = 0; i < this.listGroups.size(); i++) {
                QModelGroup group = this.listGroups.get(i);
                for (int j = 0; j < group.triIdx.length; j++) {
                    QModelTriangle t = listTri.get(group.triIdx[j]);
                    for (int k = 0; k < t.vertIdx.length; k++) {
                        QModelVertex v = this.listVertex.get(t.vertIdx[k]);
                        for (int l = 0; l < v.bones.length; l++) {
                            refBones.add(v.bones[l]);
                        }
                    }
                }
            }
            int n = 0;
            listBones = new ArrayList<>();
            Collections.sort(refBones);
            HashMap<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < loader.listBones.size(); i++) {
                if (!refBones.contains(i)) {
                    n++;
                } else {
                    map.put(i, listBones.size());
                    listBones.add(loader.listBones.get(i));
                }
            }
            for (int i = 0; i < this.listVertex.size(); i++) {
                QModelVertex v = this.listVertex.get(i);
                for (int l = 0; l < v.bones.length; l++) {
                    v.bones[l]=map.get(v.bones[l]);
                }
            }
            
        }
    }

    public void normalizeWeights(ModelLoaderQModel loader) {
        for (int i = 0; i < this.listVertex.size(); i++) {
            QModelVertex v = this.listVertex.get(i);
            if (v.numBones != 0) {
                float total = 0;
                for (int j = 0; j < v.numBones; j++) {
                    float weight = v.weights[j];
                    total += weight;
                }
                float n = 1.0f / total;
                for (int j = 0; j < v.numBones; j++) {
                    QModelBone jt = this.listBones.get(v.bones[j]);
                    if ((jt.flags & 2) == 0) {
                        throw new GameError("Invalid joint weight");
                    }
                    v.weights[j] *= n;
                }
            }
        }
    }

    protected int getMinBone(QModelTriangle o1) {
        int bones = -1;
        for (int i = 0; i < o1.vertIdx.length; i++) {
            int vertIdx = o1.vertIdx[i];
            QModelVertex vert = listVertex.get(vertIdx);
            for (int j = 0; j < vert.numBones; j++) {
                if (bones == -1) {
                    bones = vert.bones[j];
                }
                bones=Math.min(bones, vert.bones[j]);
            }
        }
        return bones;
    }


    protected int getMaxBone(QModelTriangle o1) {
        int bones = -1;
        for (int i = 0; i < o1.vertIdx.length; i++) {
            int vertIdx = o1.vertIdx[i];
            QModelVertex vert = listVertex.get(vertIdx);
            for (int j = 0; j < vert.numBones; j++) {
                if (bones == -1) {
                    bones = vert.bones[j];
                }
                bones=Math.max(bones, vert.bones[j]);
            }
        }
        return bones;
    }


    public void sortVertices(ModelLoaderQModel modelLoaderQModel) {
        Collections.sort(this.listTri, new Comparator<QModelTriangle>() {
            @Override
            public int compare(QModelTriangle o1, QModelTriangle o2) {
                int n1 = getMinBone(o1);
                int n2 = getMinBone(o2);
                if (n1 != n2) {
                    if (n1 == -1) {
                        return -1;
                    }
                    if (n2 == -1) {
                        return 1;
                    }
                    return n1 < n2 ? -1 : 1;
                }
                int m1 = getMaxBone(o1);
                int m2 = getMaxBone(o2);
                if (m1 != m2) {
                    if (m2 == -1) {
                        return -1;
                    }
                    if (m1 == -1) {
                        return 1;
                    }
                    return m1 > m2 ? -1 : 1;
                }
                return o1.idx < o2.idx ? -1 : 1;
            }
        });
    }


    public void resolveParent(ModelLoaderQModel loader) {
        if (this.parent_type == 2) {
            this.attachmentBone = loader.findJoint(this.parent_name);
        }
        if (this.parent_type == 3) {
            this.attachmentEmpty = loader.findEmpty(this.parent_name);
        }
    }


    public void resolveGroupMaterials(ModelLoaderQModel loader) {
        for (QModelGroup group : this.listGroups) {
            group.material = loader.listMaterials.get(group.materialIdx);
            if (group.material == null) {
                System.err.println("MISSING MAT FOR GRP "+this.name +" ("+group.materialIdx+")");
            }
        }
    }


    public void bindTextureIdx(int grp, int tex) {
        QModelGroup qgrp = this.listGroups.get(grp);
        QModelMaterial qmat = qgrp.material;
        int i = tex >= qmat.qTextures.length?qmat.qTextures.length-1:tex;
        qmat.setBoundTexture(qmat.qTextures[i]);
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
