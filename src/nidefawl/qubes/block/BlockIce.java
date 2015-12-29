package nidefawl.qubes.block;

import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

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
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        int data = w.getData(ix, iy, iz);
        float level = (float) Math.max(0.1, (10-data)/10.0f);
        bb.set(0, 0, 0, 1, level,1);
//        return super.getRenderBlockBounds(w, ix, iy, iz, bb);
        return bb;
    }
    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_TRANSPARENT;
    }
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        if (isVisibleBounds(w, axis, side, bb)) {
            return true;
        }
        int id = w.getType(ix, iy, iz);
        if (id==this.id || !Block.isOpaque(id)) {
            return false;
        }
//        return !w.isNormalBlock(ix, iy, iz, -1);
        return true;
    }
    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        AABBFloat bb = tmp[0];
        int data = world.getData(x,y,z);
        float level = (float) Math.max(0.1, (10-data)/10.0f);
        bb.set(0, 0, 0, 1, level,1);
        bb.offset(x, y, z);
        return 1;
    }
 
    @Override
    public boolean isFullBB() {
        return false;
    }
    @Override
    public int getRenderType() {
        return 2;
    }
}