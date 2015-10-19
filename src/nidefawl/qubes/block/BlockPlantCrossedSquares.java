/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
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
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir) {
        return super.getFaceColor(w, x, y, z, faceDir);
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
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        float size = 8/16f;
        size/=2.0f;
        bb.set(size, 0, size, 1-size, 1-1*size, 1-size);
        return bb;
    }

    
    public int getRenderShadow() {
        return 0;
    }
}
