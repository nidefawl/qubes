/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class QModelMaterial extends Vector3f {

	public String name;
	public Vector4f ambient;
	public Vector4f diffuse;
	public Vector4f specular;
	public Vector4f emissive;
	public float shininess;
	public float transparency;
	public int mode;
	public int[] texture; // [128]
	public int[] alphamap;

	public QModelMaterial() {
	}
}
