/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.List;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelTriGroup {
	public final int idx;
	public final int flags;
	public final String name;
	public final int[] triIdx;
    public final int materialIdx;
    public QModelMaterial material;
    public List<QModelTriangle> listTri;

	
	/**
     * @param i
     * @param modelLoaderQModel
	 * @throws EOFException 
     */
    public QModelTriGroup(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUByte();
        this.name = loader.readString(32);
        int numTri = loader.readUShort();
        this.triIdx = new int[numTri];
        for (int j = 0; j < numTri; j++) {
            this.triIdx[j] = loader.readUShort();
        }
        this.materialIdx = loader.readUByte();
    }


    @Override
	public String toString() {
		return "ModelMesh["+idx+", "+name+", "+triIdx.length+" Tris]";
	}
}
