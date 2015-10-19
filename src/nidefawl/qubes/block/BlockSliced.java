package nidefawl.qubes.block;

public class BlockSliced extends Block {
    public BlockSliced(int id) {
        this(id, true);
    }
    public BlockSliced(int id, boolean b) {
        super(id, b);
    }

    @Override
    public int getRenderType() {
        return 3;
    }
}
