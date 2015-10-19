/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockTorch extends Block {

    /**
     * 
     */
    public BlockTorch(int id) {
        super(id, true);
    }
    @Override
    public int getRenderType() {
        return 8;
    }
    @Override
    public int getLightValue() {
        return 14;
    }



    @Override
    public boolean applyAO() {
        return super.applyAO();
    }

    @Override
    public boolean isOccluding() {
        return false;
    }
    @Override
    public boolean isFullBB() {
        return false;
    }
    
    public int getRenderShadow() {
        return 0;
    }
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        float tmin=0.33f;
        float tmax = 1-tmin;
        bb.set(tmin, 0, tmin, tmax, 0.625f, tmax);
        int data = w.getData(ix, iy, iz);

        switch (data) {
            default:
                break;
            case 1:
                bb.offset(-0.33f, 0.2f, 0);
                break;
            case 2:
                bb.offset(0.33f, 0.2f, 0);
                break;
            case 3:
                bb.offset(0, 0.2f, -0.33f);
                break;
            case 4:
                bb.offset(0, 0.2f, 0.33f);
                break;
        }
        return bb;
    }
    
    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        return 0;
    }
}
