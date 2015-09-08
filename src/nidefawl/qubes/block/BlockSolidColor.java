package nidefawl.qubes.block;

public class BlockSolidColor extends Block {
    public BlockSolidColor(int id) {
        super(id, false);
    }

    public int getRenderPass() {
        return 0;
    }
    @Override
    public boolean applyAO() {
        return false;
    }
    
    @Override
    public int getLightValue() {
        return 15;
    }
    
    @Override
    public int getColor() {
        return 0x8888ff;
    }

    
}
