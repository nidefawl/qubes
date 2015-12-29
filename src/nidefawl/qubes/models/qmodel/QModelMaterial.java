/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.vec.Vector4f;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class QModelMaterial {

	private static final int FLAG_TEXTURED = 1;
    public final String name;
	public final Vector4f ambient;
	public final Vector4f diffuse;
	public final Vector4f specular;
	public final Vector4f emissive;
	public final float specular_hardness;
	public final float transparency;
	public final int mode;
	public final String texture;
	public final int idx;

	public QModelMaterial(int i, ModelLoaderQModel loader) throws EOFException {
	    this.idx = i;
        this.name = loader.readString(32);
        this.ambient = loader.readVec4();
        this.diffuse = loader.readVec4();
        this.specular = loader.readVec4();
        this.emissive = loader.readVec4();
        this.specular_hardness = loader.readFloat();
        this.transparency = loader.readFloat();
        this.mode = loader.readUByte();
        if ((this.mode & FLAG_TEXTURED) != 0)
            this.texture = loader.readString(32);
        else this.texture = null;
        System.out.println(loader.getModelName()+" - mode "+this.mode+", texture "+idx+": "+this.texture);
	}
}
