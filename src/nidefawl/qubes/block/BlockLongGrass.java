/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLongGrass extends BlockPlantCrossedSquares {

    /**
     * @param id
     */
    public BlockLongGrass(int id) {
        super(id);
    }

    @Override
    public boolean applyRandomOffset() {
        return true;
    }
    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_LOD;
    }
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir) {
        return Block.grass.getFaceColor(w, x, y, z, Dir.DIR_POS_Y);
    }
}