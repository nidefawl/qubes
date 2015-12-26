package nidefawl.qubes.block;

public class BlockSliced extends Block {
    public BlockSliced(String id) {
        this(id, true);
    }
    public BlockSliced(String id, boolean b) {
        super(id, b);
    }

    @Override
    public int getRenderType() {
        return 3;
    }
}
