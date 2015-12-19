package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;

public class BlockModelled extends Block {

    public BlockModelled(int id, boolean transparent) {
        this(id);
    }

    public BlockModelled(int id) {
        super(id, true);
    }

    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        return super.getRenderBlockBounds(w, ix, iy, iz, bb);
    }

    @Override
    public boolean isFullBB() {
        return super.isFullBB();
    }
    @Override
    public boolean isTransparent() {
        return super.isTransparent();
    }
    @Override
    public int getLODPass() {
        return super.getLODPass();
    }
    @Override
    public int getRenderPass() {
        return super.getRenderPass();
    }
    @Override
    public int getRenderShadow() {
        return super.getRenderShadow();
    }
    @Override
    public boolean isOccluding() {
        return super.isOccluding();
    }

    @Override
    public int getRenderType() {
        return 13;
    }
}
