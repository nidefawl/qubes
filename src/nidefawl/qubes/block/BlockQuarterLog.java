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
public class BlockQuarterLog extends Block {

    /**
     * @param id
     * @param transparent
     */
    public BlockQuarterLog(String id, boolean transparent) {
        super(id, transparent);
    }

    /**
     * @param id
     */
    public BlockQuarterLog(String id) {
        super(id);
    }

    
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idx = 0;
        switch (faceDir) {
            case Dir.DIR_POS_Z:
                idx = 1;
                break;
            case Dir.DIR_POS_X:
                idx = 3;
                break;
            case Dir.DIR_NEG_X:
                idx = 2;
                break;
            case Dir.DIR_NEG_Z:
                idx = 4;
                break;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }
}
