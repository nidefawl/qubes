package nidefawl.qubes.block;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.World;

public class BlockWater extends Block {
    public BlockWater(int id) {
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
        return 1f;
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
}
