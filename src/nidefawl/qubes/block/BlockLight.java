package nidefawl.qubes.block;

public class BlockLight extends Block {
    public BlockLight(int id) {
        super(id, false);
    }

    public int getRenderPass() {
        return 0;
    }
    @Override
    public boolean applyAO() {
        return true;
    }
    
    @Override
    public int getColor() {
        return 0x8888ff;
    }

    public float getAlpha() {
        return 1f;
    }
}
