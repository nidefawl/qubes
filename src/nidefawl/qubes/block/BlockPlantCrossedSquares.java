/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockPlantCrossedSquares extends Block {

    /**
     * @param id
     */
    public BlockPlantCrossedSquares(int id) {
        super(id, true);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getColorFromSide(int side) {
        return super.getColorFromSide(side);
    }

    public int getRenderType() {
        return 1;
    }

    @Override
    public boolean applyAO() {
        // TODO Auto-generated method stub
        return super.applyAO();
    }

    @Override
    public boolean isOccluding() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getCollisionBB(nidefawl.qubes.world.World, int, int, int, nidefawl.qubes.vec.AABB)
     */
    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] aabb) {
        return 0;
    }

    public boolean isReplaceable() {
        return true;
    }
    @Override
    public boolean isFullBB() {
        return false;
    }
    public boolean applyRandomOffset() {
        return false;
    }
}
