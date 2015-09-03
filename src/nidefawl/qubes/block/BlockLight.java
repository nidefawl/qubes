package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.world.World;

public class BlockLight extends Block {
public BlockLight(int id) {
    super(id, true);
}

    public int getRenderPass() {
        return -1;
    }

    @Override
    public boolean applyAO() {
        return false;
    }

    public AABB getCollisionBB(World world, int x, int y, int z, AABB aabb) {
        return null;
    }
}