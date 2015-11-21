/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelVertex extends Vector3f {
    final static int MAX_BONES = 8;
	public int idx;
	public int flags;
	public int refCount;
    public int bones[] = new int[MAX_BONES];
	public float weights[] = new float[MAX_BONES];
	public int numBones = 0;
    public Vector3f local = new Vector3f();
	public QModelVertex() {
	}
	/**
	 * @param readUByte
	 * @param readFloat
	 */
	public void addBoneWeight(int boneIdx, float weight) {
	    if (numBones<MAX_BONES) {
	        bones[numBones] = boneIdx;
	        weights[numBones] = weight;
	        numBones++;
	    } else {
	        System.err.println("more than "+MAX_BONES+" bones, implement sorting");
	    }
	}
}
