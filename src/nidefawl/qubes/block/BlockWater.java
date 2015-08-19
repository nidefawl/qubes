package nidefawl.qubes.block;

public class BlockWater extends Block {
    public BlockWater(int id) {
        super(id, true);
    }

    public int getRenderPass() {
        return 1;
    }
    @Override
    public boolean applyAO() {
        return false;
    }
}
