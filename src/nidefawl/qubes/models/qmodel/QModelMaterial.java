/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class QModelMaterial extends Vector3f {

	public final String name;
	public final Vector4f ambient;
	public final Vector4f diffuse;
	public final Vector4f specular;
	public final Vector4f emissive;
	public final float shininess;
	public final float transparency;
	public final int mode;
	public final int[] texture; // [128]
	public final int[] alphamap;
    private final int idx;

	public QModelMaterial(int i, ModelLoaderQModel loader) throws EOFException {
	    this.idx = i;
        this.name = loader.readString(32);
        this.ambient = loader.readVec4();
        this.diffuse = loader.readVec4();
        this.specular = loader.readVec4();
        this.emissive = loader.readVec4();
        this.shininess = loader.readFloat();
        this.transparency = loader.readFloat();
        this.mode = loader.readUByte();
        this.texture = loader.readUByteArray(128);
        this.alphamap = loader.readUByteArray(128);
	}
}
