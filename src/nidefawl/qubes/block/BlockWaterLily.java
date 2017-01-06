/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.util.Flags;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockWaterLily extends BlockPlantCrossedSquares {

    /**
     * @param id
     */
    public BlockWaterLily(String id) {
        super(id, false);
        setCategory(BlockCategory.FLOWER);
    }

    public int getRenderType() {
        return 12;
    }
    public void onUpdate(World w, int ix, int iy, int iz, int from) {
        if (!canStayOn(w, ix, iy-1, iz)) {
            w.setType(ix, iy, iz, 0, Flags.MARK|Flags.LIGHT);
        }
    }
    public boolean canStayOn(World w, int x, int y, int z) {
        return w.getWater(x, y, z)>0;
    }

}
