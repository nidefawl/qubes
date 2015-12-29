/**
 * 
 */
package nidefawl.qubes.chunk;

import java.io.IOException;

import nidefawl.qubes.nbt.Tag;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ChunkData {
    public abstract boolean setByte(int i, int j, int k, boolean upper, int val);
    public abstract short get(int i, int j, int k);
    public boolean setUpper(int i, int j, int k, int data) {
        return setByte(i, j, k, true, data);
    }
    public boolean setLower(int i, int j, int k, int data) {
        return setByte(i, j, k, false, data);
    }
    /**
     * @return
     */
    public Tag.Compound writeToTag() {
        return null;
    }
    /**
     * @param list
     */
    public void readFromTag(Tag.Compound list) throws IOException {
    }

}
