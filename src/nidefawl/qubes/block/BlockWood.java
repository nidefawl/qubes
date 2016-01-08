package nidefawl.qubes.block;

public class BlockWood extends Block {

    private int index;

    public BlockWood(String string, int i) {
        super(string);
        this.index = i;
    }
    public int getIndex() {
        return this.index;
    }

}
