package nidefawl.qubes.block;

public class BlockLight extends Block {
    public BlockLight(int id) {
        super(id, false);
    }

    @Override
    public boolean applyAO() {
        return false;
    }

    @Override
    public int getLightValue() {
        return 15;
    }
}