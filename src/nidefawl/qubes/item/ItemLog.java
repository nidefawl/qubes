package nidefawl.qubes.item;

public class ItemLog extends Item {

    private int index;

    public ItemLog(String string, int i) {
        super(string);
        this.index = i;
    }
    
    public int getIndex() {
        return this.index;
    }

}
