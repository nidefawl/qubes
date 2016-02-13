/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.io.IOException;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
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
	public final int idx;
    public final int[] textureIdxs;
    public final QModelTexture[] qTextures;
    public QModelTexture bound;

	public QModelMaterial(int i, ModelLoaderQModel loader) throws IOException {
	    this.idx = i;
        this.name = loader.readString(32);
        this.ambient = loader.readVec4();
        this.diffuse = loader.readVec4();
        this.specular = loader.readVec4();
        this.emissive = loader.readVec4();
        this.specular_hardness = loader.readFloat();
        this.transparency = loader.readFloat();
        this.mode = loader.readUByte();
        int nTextures = loader.readUByte();
        if (nTextures <0 || nTextures > 32) {
            throw new IOException("Invalid range");
        }
        this.textureIdxs = new int[nTextures];
        this.qTextures = new QModelTexture[nTextures];
        for (int t = 0; t < nTextures; t++) {
            textureIdxs[t] = loader.readUByte();
        }
//        System.out.println(loader.getModelName()+" - mode "+this.mode+", texture "+idx+": "+this.texture);
	}

    public void resolveTextures(ModelLoaderQModel loader) {
        for (int j = 0; j < this.textureIdxs.length; j++) {
            int texIdx = this.textureIdxs[j];
            this.qTextures[j] = loader.listTextures.get(texIdx);
            if (this.qTextures[j] == null) {
                System.err.println("MISSING TEX FOR MAT "+this.name +" ("+texIdx+")");
            }
        }
    }

    public void setBoundTexture(QModelTexture t) {
        bound = t;
    }


    public QModelTexture getBoundTexture() {
        if (bound != null) {
            return bound;
        }
        if (this.qTextures.length == 0) {
            return null;
        }
        return this.qTextures[0];
    }

}
