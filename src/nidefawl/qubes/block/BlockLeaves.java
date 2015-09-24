/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLeaves extends Block {

    /**
     * @param id
     */
    BlockLeaves(int id) {
        super(id, true);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getTextureFromSide(int)
     */
    @Override
    public int getTextureFromSide(int faceDir) {
        return super.getTextureFromSide(faceDir);
    }
    
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getColorFromSide(int side) {
        // TODO Auto-generated method stub
        return super.getColorFromSide(side);
    }
}
