package nidefawl.qubes.item;

public class ItemStone extends Item {

    private int index;

    public ItemStone(String string, int i) {
        super(string);
        this.index = i;
    }
    
    public int getIndex() {
        return this.index;
    }

}
