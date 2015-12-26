/**
 * 
 */
package nidefawl.qubes.hex;

import java.util.Collection;
import java.util.HashMap;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.worldgen.biome.HexBiome;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class HexagonGridStorage<T> extends HexagonGrid {
    public static class HexCellEnd<T> extends HexCell<T> {
        public HexCellEnd(HexagonGridStorage<T> grid, int x, int z) {
            super(grid, x, z);
        }
    }
    public final int max;
    /**
     * @param radius
     */
    public HexagonGridStorage(double radius, int max) {
        super(radius);
        this.max = max;
    }
    //TODO: replace with fixed size array
    HashMap<Long, T> map = new HashMap<>();
    HashMap<Long, T> oobmap = new HashMap<>();

    public void reset() {
        map.clear();
        oobmap.clear();
    }

    protected void putPos(long pos, T cell) {
        map.put(pos, cell);
    }

    public T getPos(long pos) {
        if (outOfBounds(pos)) {
            T b = this.oobmap.get(pos);
            if (b == null)
                b = oobCell(GameMath.lhToX(pos), GameMath.lhToZ(pos));
            this.oobmap.put(pos, b);
            return b;
        } 
        T b = this.map.get(pos); 
        if (b == null) {
            b = loadCell(GameMath.lhToX(pos), GameMath.lhToZ(pos));
            this.map.put(pos, b);
        }
        return b;
    }

    public boolean outOfBounds(long pos) {
        int x = GameMath.lhToX(pos);
        int z = GameMath.lhToZ(pos);
        return x < -max || x > max || z < -max || z > max;
    }

    public T blockToHex(int x, int z) {
        long pos = this.blockToGrid(x, z);
        return getPos(pos);
    }
    
    public abstract T loadCell(int x, int z);
    
    public abstract T oobCell(int x, int z);

    //TODO: check thread safety
    public Collection<T> getLoaded() {
        return this.map.values();
    }

    public void flag(int x, int z) {
    }
}
