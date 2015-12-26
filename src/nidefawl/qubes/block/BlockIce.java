package nidefawl.qubes.block;

import nidefawl.qubes.blocklight.LightChunkCache;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockIce extends Block {
    public BlockIce(String id) {
        super(id, true);
    }

    @Override
    public int getRenderPass() {
        return 1;
    }

    @Override
    public boolean applyAO() {
        return false;
    }

    @Override
    public float getAlpha() {
        return 0.87f;
    }

    @Override
    public boolean isOccluding() {
        return false;
    }
    
    @Override
    public int getRenderShadow() {
        return 0;
    }
    @Override
    public int getLightLoss(LightChunkCache c, int i, int j, int k, int type) {
        return 2;
    }
}