/**
 * 
 */
package nidefawl.qubes.hex;

import java.util.Collection;
import java.util.HashMap;

import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class HexagonGridStorage<T> extends HexagonGrid {

    /**
     * @param radius
     */
    public HexagonGridStorage(double radius) {
        super(radius);
    }
    //TODO: replace with fixed size array
    HashMap<Long, T> map = new HashMap<>();

    protected void putPos(long pos, T cell) {
        map.put(pos, cell);
    }

    public T getPos(long pos) {
        T b = this.map.get(pos); 
        if (b == null) {
            b = loadCell(GameMath.lhToX(pos), GameMath.lhToZ(pos));
            this.map.put(pos, b);
        }
        return b;
    }
    public T blockToHex(int x, int z) {
        long pos = this.blockToGrid(x, z);
        return getPos(pos);
    }
    
    public abstract T loadCell(int x, int z);

    //TODO: check thread safety
    public Collection<T> getLoaded() {
        return this.map.values();
    }
}
