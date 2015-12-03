/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelTriangle {
	public final int idx;
	public final int[] vertIdx = new int[3];
	public final Vector3f[] normal = new Vector3f[3];
	public final float[][] texCoord = new float[2][];
	public final int smoothing;
	public final int group;
	public final int flags;
	
	/**
     * @param i
     * @param modelLoaderQModel
	 * @throws EOFException 
     */
    public QModelTriangle(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUShort();
        this.vertIdx[0] = loader.readUShort();
        this.vertIdx[1] = loader.readUShort();
        this.vertIdx[2] = loader.readUShort();
//        tmpVec.x, -tmpVec.z, tmpVec.y
        Vector3f faceNormal1 = loader.readVec3();
        Vector3f faceNormal2 = loader.readVec3();
        Vector3f faceNormal3 = loader.readVec3();
        this.normal[0] = new Vector3f(faceNormal1.x, faceNormal1.y, faceNormal1.z);
        this.normal[1] = new Vector3f(faceNormal2.x, faceNormal2.y, faceNormal2.z);
        this.normal[2] = new Vector3f(faceNormal3.x, faceNormal3.y, faceNormal3.z);
        for (int j = 0; j < 2; j++) {
            this.texCoord[1-j] = new float[3];
            this.texCoord[1-j][0] = loader.readFloat();
            this.texCoord[1-j][1] = loader.readFloat();
            this.texCoord[1-j][2] = loader.readFloat();
        }
        this.smoothing = loader.readUByte();
        this.group = loader.readUByte();
    }

    @Override
	public String toString() {
		return "ModelTri["+idx+", ("+vertIdx[0]+","+vertIdx[1]+","+vertIdx[2]+")]";
	}
}
