/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelVertex extends Vector3f {
    final static int MAX_BONES = 8;
	public final int idx;
	public final int flags;
	public final int refCount;
    public final int bones[] = new int[MAX_BONES];
	public final float weights[] = new float[MAX_BONES];
	public final int numBones;
	/**
     * @param i
     * @param modelLoaderQModel
	 * @throws EOFException 
     */
    public QModelVertex(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUByte();
        this.x = loader.readFloat();
        this.y = loader.readFloat();
        this.z = loader.readFloat();
        int nBones = loader.readUByte();
        int idx = 0;
        for (int j = 0; j < nBones; j++) {
            int boneIdx = loader.readUByte();
            float weight = loader.readFloat();
            if (weight > 1E-4f) {
                if (idx >= MAX_BONES) {
                    throw new GameError("Too many bone weights on vertex #"+i+" -> "+nBones);
                }
                bones[idx] = boneIdx;
                weights[idx] = weight;
                idx++;
            }
        }
        this.numBones = idx;
        this.refCount = loader.readUByte();
    }
}
