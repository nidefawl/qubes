/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.ItemTextureArray;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemStack extends BaseStack {

    public int id;
    public int data;
    public int size;
    
    public ItemStack(Item item) {
        this.id = item.id;
        this.size = 1;
        this.data = 0;
    }

    public ItemStack(Tag.Compound t) {
        this.id = t.getInt("id");
        this.data = t.getInt("data");
        this.size = t.getInt("size");
    }
    public ItemStack() {
    }

    /**
     * @param t
     */
    public ItemStack(Tag t) {
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

    /**
     * @return
     */
    public int getItemTexture() {
        return getItem().getTexture(this);
    }

    /**
     * @return
     */
    private Item getItem() {
        return Item.get(this.id);
    }

    @Override
    public BaseStack copy() {
        ItemStack stack = new ItemStack();
        stack.id = this.id;
        stack.data = this.data;
        stack.size = this.size;
        return stack;
    }

    @Override
    public Tag save() {
        Tag.Compound tag = new Tag.Compound();
        tag.setInt("type", 1);
        tag.setInt("id", this.id);
        tag.setInt("data", this.data);
        tag.setInt("size", this.size);
        return tag;
    }

}
