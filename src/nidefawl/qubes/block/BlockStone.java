package nidefawl.qubes.block;

public class BlockStone extends Block {

    public BlockStone(String name, boolean transparent) {
        super(name, transparent);
    }

    public BlockStone(String id) {
        super(id);
    }
    
    @Override
    public float getRoughness(int texture) {
        return 0.2f;
    }

}
