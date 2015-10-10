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
public class BlockLog extends Block {

    /**
     * @param id
     */
    public BlockLog(int id) {
        super(id, false);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getTextureFromSide(int)
     */
    @Override
    public int getTexture(int faceDir, int dataVal) {
        return BlockTextureArray.getInstance().getTextureIdx(this.id, Dir.isTopBottom(faceDir) ? 1 : 0);
    }
}
