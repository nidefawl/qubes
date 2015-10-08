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
    
    final int leavesColor;
    /**
     * @param id
     */
    public BlockLeaves(int id) {
        this(id, -1);
    }

    public BlockLeaves(int id, int rgb) {
        super(id, true);
        this.leavesColor = rgb;
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
        return leavesColor;
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#applyAO()
     */
    @Override
    public boolean applyAO() {
        return true;
    }
    @Override
    public boolean isOccluding() {
        // TODO Auto-generated method stub
        return true;//super.isOccluding();
    }
}
