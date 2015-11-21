/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelMesh {
	public int idx;
	public int flags;
	public String name;
	public int[] triIdx;
	public int material;
	public float jointSize;
	public int transparencyMode;
	public float readFloat;

	
	@Override
	public String toString() {
		return "ModelMesh["+idx+", "+name+", "+triIdx.length+" Tris]";
	}
}
