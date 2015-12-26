package nidefawl.qubes.block;

public class BlockLit extends Block {
    private final int lightValue;

    public BlockLit(String id) {
        this(id, 15);
    }
    public BlockLit(String id, int lightValue) {
        super(id, false);
        this.lightValue = lightValue;
    }

    @Override
    public boolean applyAO() {
        return true;
    }
    
    @Override
    public int getLightValue() {
        return lightValue;
    }
    
}
