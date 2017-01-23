/**
 * 
 */
package nidefawl.qubes.models.qmodel.loader;

import java.io.*;
import java.util.*;

import com.google.common.collect.Lists;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.io.BinaryStreamReader;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.util.GameError;

/**
 * Loads models. Abstracts model data and layout from rendering.
 * Models reference data from here to reduce memory footprint.
 * 
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class ModelLoaderQModel extends BinaryStreamReader {


	/**
	 * 
	 */
	public ModelLoaderQModel() {
	}

	final static String HEADER = "qmodel0000";

	public List<QModelObject> listObjects;
    public List<QModelMaterial> listMaterials;
    public List<QModelTexture> listTextures;
    public List<QModelBone> listBones;
    public List<QModelAction> listActions;
    public List<QModelNode> listEmpties;
	private String path;
    private QModelType modelType;

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
			if (version != 8) {
				throw new IOException("version != 8");
			}
			int iModelType = readUByte();
            this.modelType = QModelType.get(iModelType);
            if (this.modelType == null) {
                throw new IOException("Invalid modelType '"+iModelType+"'");
            }
            int numObjects = readUShort();
            listObjects = Lists.newArrayListWithExpectedSize(numObjects);
            for (int i = 0; i < numObjects; i++) {
                QModelObject v = new QModelObject(i, this);
                listObjects.add(v);
//                System.out.println(v.name+", parent: "+v.parent_name+", parent-type: "+v.parent_type);
            }
            int numNodes = readUShort();
            listEmpties = Lists.newArrayListWithExpectedSize(numNodes);
            for (int i = 0; i < numNodes; i++) {
                QModelNode v = new QModelNode(i, this);
                listEmpties.add(v);
//                System.out.println(v.name+", parent: "+v.parent_name+", parent-type: "+v.parent_type);
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
            for (int i = 0; i < numMaterials; i++) {
                QModelMaterial mat = this.listMaterials.get(i);
                mat.resolveTextures(this);
            }
            for (int o = 0; o < this.listObjects.size(); o++) {
                QModelObject obj = this.listObjects.get(o);
                obj.flattenBoneList(this);
                obj.resolveParent(this);
                obj.resolveGroupMaterials(this);
                obj.normalizeWeights(this);
                obj.sortVertices(this);
            }
            for (int o = 0; o < this.listEmpties.size(); o++) {
                QModelNode obj = this.listEmpties.get(o);
                obj.resolveParent(this);
            }

//            Collections.reverse(this.listTri);
//            for (int i = 0; i < listTri.size(); i++) {
//                QModelTriangle tri = listTri.get(i);
//                int n1 = getMinBone(tri);
//                int m1 = getMaxBone(tri);
//                System.out.println("["+i+"] = "+tri.idx+" - ("+debugStrBones(tri)+")");
//            }
//            System.exit(1);
			
		} catch (Exception e) {
			throw new GameError("Failed loading model "+path, e);
		}
		
	}



//    private String debugStrBones(QModelTriangle tri) {
//        List<Integer> intList = Lists.newArrayList(); 
//        for (int i = 0; i < 3; i++) {
//            QModelVertex v = this.getVertex(tri.vertIdx[i]);
//            for (int j = 0; j < v.numBones; j++) {
//                if (!intList.contains(v.bones[j])) {
//                    intList.add(v.bones[j]);
//                }
//            }
//        }
//        if (intList.isEmpty()) {
//            return "empty";
//        }
//        Collections.sort(intList);
//        String s = "";
//        for (int a = 0; a < intList.size(); a++) {
//            if (a > 0) {
//                s+=",";
//            }
//            s+=intList.get(a);
//        }
//        return s;
//    }


    /**
     * @param parent
	 * @return 
     */
    public QModelBone findJoint(String parent) {
        for (int i = 0; i < this.listBones.size(); i++) {
            QModelBone joint = this.listBones.get(i);
            if (joint.name.equals(parent)) {
                return joint;
            }
        }
        return null;
    }
    public QModelNode findEmpty(String parent) {
        for (int i = 0; i < this.listEmpties.size(); i++) {
            QModelNode obj = this.listEmpties.get(i);
            if (obj.name.equals(parent)) {
                return obj;
            }
        }
        return null;
    }

    /**
	 * 
	 */
	private void printInfo() {
		System.out.println("loaded " + path + ": " + this.asset.getData().length + " bytes");
		System.out.println("objects: "+this.listObjects.size());
		System.out.println("materials: "+this.listMaterials.size());
        System.out.println("Joints: "+this.listBones.size());
        System.out.println("Action: "+this.listActions.size());
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

    public String getModelName() {
        String substr = this.asset.getName();
        int n1 = substr.indexOf("/");
        int n2 = substr.lastIndexOf(".");
        if (n1 >= 0 && n1+1<substr.length() && n2 >= 0) {
            substr = substr.substring(n1+1, n2);
        }
        return substr;
    }
}
