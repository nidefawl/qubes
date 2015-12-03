/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockFlowerFMN extends BlockPlantCrossedSquares {

    /**
     * @param id
     * @param multipass
     */
    public BlockFlowerFMN(int id) {
        super(id, true);
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        if (multipass && pass != 1) {
            return Block.grass.getFaceColor(w, x, y, z, Dir.DIR_POS_Y, pass);    
        }
        return super.getFaceColor(w, x, y, z, faceDir, pass);
    }
    public int getTexturePasses() {
        return multipass?3:1;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        if (multipass) {
            return BlockTextureArray.getInstance().getTextureIdx(this.id, pass);
        }
        return super.getTexture(faceDir, dataVal, pass);
    }


}
