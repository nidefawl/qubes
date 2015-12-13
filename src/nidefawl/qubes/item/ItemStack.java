/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemStack extends BaseStack {

    public int id;
    public int data;
    public int size;
    
    public ItemStack(Item pickaxe) {
        this.id = pickaxe.id;
        this.size = 1;
        this.data = 0;
    }

    @Override
    public void read(DataInput in) throws IOException {
        this.id = in.readInt();
        this.data = in.readInt();
        this.size = in.readInt();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeInt(this.data);
        out.writeInt(this.size);
    }

    @Override
    public boolean isItem() {
        return true;
    }

}
