/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelMesh {
	public final int idx;
	public final int flags;
	public final String name;
	public final int[] triIdx;
	public final int material;

	
	/**
     * @param i
     * @param modelLoaderQModel
	 * @throws EOFException 
     */
    public QModelMesh(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUByte();
        this.name = loader.readString(32);
        int numTri = loader.readUShort();
        this.triIdx = new int[numTri];
        for (int j = 0; j < numTri; j++) {
            this.triIdx[j] = loader.readUShort();
        }
        this.material = loader.readUByte();
        
    }


    @Override
	public String toString() {
		return "ModelMesh["+idx+", "+name+", "+triIdx.length+" Tris]";
	}
}
