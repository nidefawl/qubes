package nidefawl.qubes.block;

public class BlockGlowStone extends Block {
    public BlockGlowStone(int id) {
        super(id, false);
    }

    @Override
    public boolean applyAO() {
        return true;
    }
    
    @Override
    public int getLightValue() {
        return 15;
    }
    
}
