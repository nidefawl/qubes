package nidefawl.qubes.block;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.World;

public class BlockWater extends Block {
    public BlockWater(String id) {
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
    public boolean isReplaceable() {
        return true;
    }

    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        return 0;
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
