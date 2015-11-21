/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelTriangle {
	public int idx;
	public int[] vertIdx = new int[3];
	public Vector3f[] normal = new Vector3f[3];
	public float[][] texCoord = new float[2][];
	public int smoothing;
	public int group;
	public int flags;
	
	@Override
	public String toString() {
		return "ModelTri["+idx+", ("+vertIdx[0]+","+vertIdx[1]+","+vertIdx[2]+")]";
	}
}
