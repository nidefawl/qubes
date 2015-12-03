/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.*;
import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.util.GameError;
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
    public List<QModelBone> listBones;
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
    public void loadAnimation(String path) {
        
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
			int numMeshes = readUShort();
			listMesh = Lists.newArrayListWithCapacity(numMeshes);
			
			for (int i = 0; i < numMeshes; i++) {
				QModelMesh mesh = new QModelMesh(i, this);
				listMesh.add(mesh);
			}
			int numMaterials = readUShort();
			listMaterials = Lists.newArrayListWithCapacity(numMaterials);
			for (int i = 0; i < numMaterials; i++) {
				QModelMaterial mat = new QModelMaterial(i, this);
				listMaterials.add(mat);
			}
			this.fps = readFloat();
			this.curTime = readFloat();
			this.totalFrames = readInt();
			
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
		System.out.println("meshes: "+this.listMesh.size());
		System.out.println("materials: "+this.listMaterials.size());
		System.out.println("Joints: "+this.listBones.size());
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
		ModelQModel model = new ModelQModel(this);
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
