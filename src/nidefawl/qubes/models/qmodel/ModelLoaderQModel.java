/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.*;
import java.util.*;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ModelLoaderQModel {

	private ModelQModel model;

	/**
	 * 
	 */
	public ModelLoaderQModel() {
	}

	int offset = 0;
	public AssetBinary asset;
	public float fps;
	public float curTime;
	public int totalFrames;
	final static String HEADER = "qmodel0000";

	public List<QModelVertex> listVertex;
	public List<QModelTriangle> listTri;
	public List<QModelMesh> listMesh;
	public List<QModelMaterial> listMaterials;
	public List<QModelJoint> listJoints;
	private String path;

	byte[] readBytes(int len) throws EOFException {
    	byte[] data = this.asset.getData();
    	if (offset+len-1>=data.length)
    		throw new EOFException();
		byte[] rawdata = new byte[len];
		System.arraycopy(data, this.offset, rawdata, 0, len);
		this.offset += len;
		return rawdata;
	}

	private int[] readUByteArray(int i) throws EOFException {
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
		try {
			this.path = path;

			resetOffset();
			this.asset = AssetManager.getInstance().loadBin(path);
			String header = readString(10);
			if (!HEADER.equals(header)) {
				throw new IOException("Invalid model header '"+header+"'");
			}
			int version = readInt();
			if (version != 4) {
				throw new IOException("version != 4");
			}
			int numVertices = readUShort();
			int maxBoneIDFromVertexWeight = -1;
			listVertex = Lists.newArrayListWithCapacity(numVertices);
			for (int i = 0; i < numVertices; i++) {
				QModelVertex v = new QModelVertex();
				v.idx = i;
				v.flags = readUByte();
				v.x = readFloat();
				v.y = readFloat();
				v.z = readFloat();
				int numBones = readUByte();
				for (int j = 0; j < numBones; j++) {
					int boneIdx = readUByte();
					float f = readFloat();
					if (f > 1E-2F) {
	                    v.addBoneWeight(boneIdx, f);
	                    if (boneIdx > maxBoneIDFromVertexWeight) {
	                        maxBoneIDFromVertexWeight = boneIdx;
	                    }   
					}
				}
                if (v.numBones == 1) {
                    if (v.weights[0] > 0) {
                        v.weights[0] = 1;
                    }
                }
				v.refCount = readUByte();
				listVertex.add(v);
			}
			int numTriangles = readUShort();
			listTri = Lists.newArrayListWithCapacity(numTriangles);
			for (int i = 0; i < numTriangles; i++) {
				QModelTriangle tri = new QModelTriangle();
				tri.idx = i;
				tri.flags = readUShort();
				tri.vertIdx[0] = readUShort();
				tri.vertIdx[1] = readUShort();
				tri.vertIdx[2] = readUShort();
				tri.normal[0] = readVec3();
				tri.normal[1] = readVec3();
				tri.normal[2] = readVec3();
				for (int j = 0; j < 2; j++) {
					tri.texCoord[1-j] = new float[3];
					tri.texCoord[1-j][0] = readFloat();
					tri.texCoord[1-j][1] = readFloat();
					tri.texCoord[1-j][2] = readFloat();
				}
				tri.smoothing = readUByte();
				tri.group = readUByte();
				listTri.add(tri);
			}
			int numMeshes = readUShort();
			listMesh = Lists.newArrayListWithCapacity(numMeshes);
			
			for (int i = 0; i < numMeshes; i++) {
				QModelMesh mesh = new QModelMesh();
				mesh.idx = i;
				mesh.flags = readUByte();
				mesh.name = readString(32);
				int numTri = readUShort();
				mesh.triIdx = new int[numTri];
				for (int j = 0; j < numTri; j++) {
					mesh.triIdx[j] = readUShort();
				}
				mesh.material = readUByte();
				listMesh.add(mesh);
			}
			int numMaterials = readUShort();
			listMaterials = Lists.newArrayListWithCapacity(numMaterials);
			for (int i = 0; i < numMaterials; i++) {
				QModelMaterial mat = new QModelMaterial();
				mat.name = readString(32);
				mat.ambient = readVec4();
				mat.diffuse = readVec4();
				mat.specular = readVec4();
				mat.emissive = readVec4();
				mat.shininess = readFloat();
				mat.transparency = readFloat();
				mat.mode = readUByte();
				mat.texture = readUByteArray(128);
				mat.alphamap = readUByteArray(128);
				listMaterials.add(mat);
			}
			this.fps = readFloat();
			this.curTime = readFloat();
			this.totalFrames = readInt();
			
			int numJoints = readUShort();
			listJoints = Lists.newArrayListWithCapacity(numJoints);
			for (int i = 0; i < numJoints; i++) {
				QModelJoint jt = new QModelJoint();
				jt.idx = i;
				jt.flags = readUByte();
				jt.name = readString(32);
				jt.parentName = readString(32);
				float[] mat = new float[16];
				jt.modelMat = new Matrix4f();
				for (int j = 0; j < 16; j++) {
				    mat[j] = readFloat();
				}
                jt.modelMat.load(mat);
				jt.rotation = readQuaternion();
                jt.position = readVec3();
                jt.direction = readVec3();
//                if (jt.idx == 0) {
//                    jt.modelMat.translate(jt.position.negate());
//                    jt.modelMat.rotate(90*GameMath.PI_OVER_180, 0, 0, 1);
//                    System.out.println(jt.name);
//                    jt.modelMat.translate(jt.position.negate());
//                }
				int numRotFrames = readUShort();
				int numTranslationFrames = readUShort();
				jt.animation = new QJointAnimation(numRotFrames, numTranslationFrames);
                for (int j = 0; j < numRotFrames; j++) {
                    float time = readFloat();
                    Quaternion q = readQuaternion();
                    QModelKeyframeRot frame = new QModelKeyframeRot(j, time, q);
                    jt.animation.addFrame(frame);
                }
                for (int j = 0; j < numTranslationFrames; j++) {
                    float time = readFloat();
                    Vector3f q = readVec3();
                    QModelKeyframeTrans frame = new QModelKeyframeTrans(j, time, q);
                    jt.animation.addFrame(frame);
                }
				listJoints.add(jt);
			}
            for (int i = 0; i < numJoints; i++) {

                QModelJoint jt = listJoints.get(i);
//                System.out.println(jt.name);
                jt.parent = findJoint(jt.parentName);
                jt.matLocal = new Matrix4f();
                jt.matAbs = new Matrix4f();
                jt.matFinal = new Matrix4f();
                jt.matFinal2 = new Matrix4f();
                jt.matLocal.setIdentity();
                jt.matFinal.setIdentity();
                jt.matAbs.setIdentity();
//                if (jt.parent != null)
                    jt.matLocal.translate(jt.position);
                jt.matLocal.setFromQuat(jt.rotation.x, jt.rotation.y,jt.rotation.z, jt.rotation.w);
//                jt.matLocal.load(jt.modelMat);
                if (jt.parent != null) {
                    if (jt.parent.matAbs == null) {
                        throw new GameError("joints out of order!");
                    }
                    jt.matAbs.load(jt.matLocal);
                    jt.matAbs.mulMat(jt.parent.matAbs);
                    jt.parent.addChild(jt);
                } else {
                    jt.matAbs.load(jt.matLocal);
                }
                jt.matFinal.load( jt.matAbs );
                jt.matFinal2.load( jt.matAbs );
            }
            Matrix4f rootInverse = new Matrix4f();
            rootInverse.load(listJoints.get(0).matAbs);
            rootInverse.invert();
			for (int i = 0; i < numVertices; i++) {
			    QModelVertex v = this.listVertex.get(i);
			    if (v.numBones == 0) {
                    v.matModelToBoneSpace=rootInverse;
//                    Matrix4f.transform(rootInverse, v, v);
			    } else {
			        Matrix4f m = new Matrix4f();
			        m.setZero();
			        float total = 0;
                    for (int j = 0; j < v.numBones; j++) {
                        QModelJoint jt = this.listJoints.get(v.bones[j]);
                        float weight = v.weights[j];
                        total+=weight;
                        if (j == v.numBones-1) {
                            if (total < 1) {
                                weight += 1-total;
                            }
                        }
                        m.addWeighted(jt.matAbs, weight);
                    }
                    m.invert();
                    v.matModelToBoneSpace = m;
                    Matrix4f.transform(m, v, v);
			    }
			}
			if (maxBoneIDFromVertexWeight+1 > numJoints) {
				throw new GameError("Bone weight idx does not match number of joints");
			}
			
			//SHOUDL BE SORTED
//			Collections.sort(this.listVertex, new Comparator<QModelVertex>() {
//				@Override
//				public int compare(QModelVertex o1, QModelVertex o2) {
//					return o1.idx;
//				}
//			});
//			printInfo();
		} catch (Exception e) {
			throw new GameError("Failed loading model "+path, e);
		}
		
	}


	/**
     * @param parent
	 * @return 
     */
    QModelJoint findJoint(String parent) {
        for (int i = 0; i < this.listJoints.size(); i++) {
            QModelJoint joint = this.listJoints.get(i);
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
		System.out.println("meshes: "+this.listMesh.size());
		System.out.println("materials: "+this.listMaterials.size());
		System.out.println("Joints: "+this.listJoints.size());
		System.out.println("FPS: "+this.fps);
		System.out.println("CurTime: "+this.curTime);
		System.out.println("Frames: "+this.totalFrames);
	}

	/**
	 * 
	 */
	private void resetOffset() {
		this.offset = 0;
	}

	public ModelQModel buildModel() {
		ModelQModel model = new ModelQModel(this.listJoints.get(0));
		model.loader = this;
		return model;
	}

	/**
	 * @param i
	 * @return
	 */
	public QModelVertex getVertex(int i) {
		return this.listVertex.get(i);
	}
}
