package nidefawl.qubes.item;

public class ItemPlank extends Item {

    private int index;

    public ItemPlank(String string, int i) {
        super(string);
        this.index = i;
    }
    
    public int getIndex() {
        return this.index;
    }

}
