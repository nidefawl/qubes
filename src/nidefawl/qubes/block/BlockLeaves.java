/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

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
    
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir) {
        return leavesColor;
    }

    @Override
    public boolean applyAO() {
        return true;
    }
    
    @Override
    public boolean isOccluding() {
        return true;
    }
    
    public int getLightLoss(LightChunkCache c, int i, int j, int k, int type) {
        return 3;
    }
}
