/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.*;
import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.*;

/**
 * Loads models. Abstracts model data and layout from rendering.
 * Models reference data from here to reduce memory footprint.
 * 
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class ModelLoaderQModel {


	/**
	 * 
	 */
	public ModelLoaderQModel() {
	}

	int offset = 0;
	public AssetBinary asset;
	final static String HEADER = "qmodel0000";

	public List<QModelVertex> listVertex;
	public List<QModelTriangle> listTri;
	public List<QModelTriGroup> listGroups;
    public List<QModelMaterial> listMaterials;
    public List<QModelTexture> listTextures;
    public List<QModelBone> listBones;
    public List<QModelAction> listActions;
    public Map<String, QModelTriGroup> mapGroups;
    public Map<String, QModelMaterial> mapMaterials;
	private String path;
    private QModelType modelType;

	byte[] readBytes(int len) throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset+len-1>=data.length)
    		throw new EOFException();
		byte[] rawdata = new byte[len];
		System.arraycopy(data, this.offset, rawdata, 0, len);
		this.offset += len;
		return rawdata;
	}

	int[] readUByteArray(int i) throws EOFException {
		byte[] data = readBytes(i);
		int[] unsigned = new int[i];
		for (int j = 0; j < i; j++) {
			unsigned[j] = data[j] & 0xFF;
		}
		return unsigned;
	}
    public float readFloat() throws EOFException {
    	int floatBits = readInt();
    	return Float.intBitsToFloat(floatBits);
    }
    
    public Vector3f readVec3() throws EOFException {
        return new Vector3f(readFloat(), readFloat(), readFloat());
    }
    
    public Quaternion readQuaternion() throws EOFException {
        return new Quaternion(readFloat(), readFloat(), readFloat(), readFloat());
    }
    
    public Vector4f readVec4() throws EOFException {
		return new Vector4f(readFloat(), readFloat(), readFloat(), readFloat());
	}
    
    public int readInt() throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset+3>=data.length)
    		throw new EOFException();
        int ch1 = data[offset++]&0xFF;
        int ch2 = data[offset++]&0xFF;
        int ch3 = data[offset++]&0xFF;
        int ch4 = data[offset++]&0xFF;
        return ((ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }
    public int readUShort() throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset+1>=data.length)
    		throw new EOFException();
        int ch1 = data[offset++]&0xFF;
        int ch2 = data[offset++]&0xFF;
        return (ch1 << 0) + (ch2 << 8);
    }
    public int readUByte() throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset>=data.length)
    		throw new EOFException();
        return data[offset++]&0xFF;
    }
    public int readSByte() throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset>=data.length)
    		throw new EOFException();
        return data[offset++];
    }
    public String readString(int len) throws EOFException {
		byte[] strBytes = readBytes(len);
		int strlen = 0;
		for (int i = 0; i < strBytes.length && strBytes[i] != 0; i++, strlen++);
        return new String(strBytes, 0, strlen);
    }

    public void loadModel(String path) {
        AssetBinary bin = AssetManager.getInstance().loadBin(path);
        loadModel(bin);
    }
	public void loadModel(AssetBinary bin) {
		try {
		    this.asset = bin;
			this.path = bin.getPack()+":"+bin.getName();
			resetOffset();
			String header = readString(10);
			if (!HEADER.equals(header)) {
				throw new IOException("Invalid model header '"+header+"'");
			}
			int version = readInt();
			if (version != 4) {
				throw new IOException("version != 4");
			}
			int iModelType = readUByte();
            this.modelType = QModelType.get(iModelType);
            if (this.modelType == null) {
                throw new IOException("Invalid modelType '"+iModelType+"'");
            }
			int numVertices = readUShort();
			listVertex = Lists.newArrayListWithCapacity(numVertices);
			for (int i = 0; i < numVertices; i++) {
				QModelVertex v = new QModelVertex(i, this);
				listVertex.add(v);
			}
			int numTriangles = readUShort();
			listTri = Lists.newArrayListWithCapacity(numTriangles);
			for (int i = 0; i < numTriangles; i++) {
				QModelTriangle tri = new QModelTriangle(i, this);
				listTri.add(tri);
			}
			int numGroups = readUShort();
			listGroups = Lists.newArrayListWithCapacity(numGroups);
			
			for (int i = 0; i < numGroups; i++) {
				QModelTriGroup group = new QModelTriGroup(i, this);
				listGroups.add(group);
			}
            int numTextures = readUShort();
            listTextures = Lists.newArrayListWithCapacity(numTextures);
            for (int i = 0; i < numTextures; i++) {
                QModelTexture mat = new QModelTexture(i, this);
                listTextures.add(mat);
            }
            int numMaterials = readUShort();
            listMaterials = Lists.newArrayListWithCapacity(numMaterials);
            for (int i = 0; i < numMaterials; i++) {
                QModelMaterial mat = new QModelMaterial(i, this);
                listMaterials.add(mat);
            }
			
			int numJoints = readUShort();
			listBones = Lists.newArrayListWithCapacity(numJoints);
			for (int i = 0; i < numJoints; i++) {
				QModelBone jt = new QModelBone(i, this);
				listBones.add(jt);
			}
			//resolve parents
            for (int i = 0; i < numJoints; i++) {
                QModelBone jt = listBones.get(i);
                jt.parent = findJoint(jt.parentName);
                if (jt.parent != null) {
                    jt.parent.addChild(jt);
                }
            }
            int numActions = readUShort();
            listActions = Lists.newArrayListWithCapacity(numActions);
            for (int i = 0; i < numActions; i++) {
                QModelAction jt = new QModelAction(i, this);
                listActions.add(jt);
            }
            this.mapGroups = Maps.newHashMap();
            this.mapMaterials = Maps.newHashMap();
            for (int i = 0; i < numGroups; i++) {
                QModelTriGroup group = this.listGroups.get(i);
                group.listTri = Lists.newArrayList();
                for (int j = 0; j < group.triIdx.length; j++) {
                    int idx = group.triIdx[j];
                    group.listTri.add(listTri.get(idx));
                }
            }
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
//            Collections.reverse(this.listTri);
//            for (int i = 0; i < listTri.size(); i++) {
//                QModelTriangle tri = listTri.get(i);
//                int n1 = getMinBone(tri);
//                int m1 = getMaxBone(tri);
//                System.out.println("["+i+"] = "+tri.idx+" - ("+debugStrBones(tri)+")");
//            }
//            System.exit(1);
            // link instances in 1:1 relation of groups and materials
            for (QModelMaterial mat : this.listMaterials) {
                for (QModelTriGroup group : this.listGroups) {
                    if (mat.idx == group.materialIdx) {
                        group.material = mat;
                        this.mapGroups.put(mat.name, group);
                        break;
                    }
                }
                this.mapMaterials.put(mat.name, mat);
            }
            //normalize weights
			for (int i = 0; i < numVertices; i++) {
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
			
		} catch (Exception e) {
			throw new GameError("Failed loading model "+path, e);
		}
		
	}



    private String debugStrBones(QModelTriangle tri) {
        List<Integer> intList = Lists.newArrayList(); 
        for (int i = 0; i < 3; i++) {
            QModelVertex v = this.getVertex(tri.vertIdx[i]);
            for (int j = 0; j < v.numBones; j++) {
                if (!intList.contains(v.bones[j])) {
                    intList.add(v.bones[j]);
                }
            }
        }
        if (intList.isEmpty()) {
            return "empty";
        }
        Collections.sort(intList);
        String s = "";
        for (int a = 0; a < intList.size(); a++) {
            if (a > 0) {
                s+=",";
            }
            s+=intList.get(a);
        }
        return s;
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

    /**
     * @param parent
	 * @return 
     */
    QModelBone findJoint(String parent) {
        for (int i = 0; i < this.listBones.size(); i++) {
            QModelBone joint = this.listBones.get(i);
            if (joint.name.equals(parent)) {
                return joint;
            }
        }
        return null;
    }

    /**
	 * 
	 */
	private void printInfo() {
		System.out.println("loaded " + path + ": " + this.asset.getData().length + " bytes");
		System.out.println("vertices: "+this.listVertex.size());
		System.out.println("triangles: "+this.listTri.size());
		System.out.println("meshes: "+this.listGroups.size());
		System.out.println("materials: "+this.listMaterials.size());
        System.out.println("Joints: "+this.listBones.size());
        System.out.println("Action: "+this.listActions.size());
	}

	/**
	 * 
	 */
	private void resetOffset() {
		this.offset = 0;
	}
    public ModelBlock buildBlockModel() {
        return new ModelBlock(this);
    }

	public ModelQModel buildModel() {
	    switch (this.modelType) {
	        case RIGGED:
	            return new ModelRigged(this);
	        case STATIC:
            default:
	            return new ModelStatic(this);
	    }
	}

	/**
	 * @param i
	 * @return
	 */
	public QModelVertex getVertex(int i) {
		return this.listVertex.get(i);
	}

    public String getModelName() {
        return this.path;
    }

    public QModelTriGroup getGroup(String string) {
        return this.mapGroups.get(string);
    }
    public QModelMaterial getListMaterial(String string) {
        return this.mapMaterials.get(string);
    }
}
