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
        setCategory(BlockCategory.LOG);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getTextureFromSide(int)
     */
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        int rot = dataVal & 3;
        int topFace = Dir.DIR_POS_Y;
        int bottomFace = Dir.DIR_NEG_Y;
        int idx = 0;
        switch (rot) {
            case 1:
                topFace = Dir.DIR_POS_X;
                bottomFace = Dir.DIR_NEG_X;
                break;
            case 2:
                topFace = Dir.DIR_POS_Z;
                bottomFace = Dir.DIR_NEG_Z;
                break;
        }
        if (faceDir == topFace || faceDir == bottomFace) {
            idx = 1;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }
}
