/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.ByteArray;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockStack extends BaseStack {

    public StackData stackdata;
    
    /**
     * @param stackdata the stackdata to set
     */
    public void setStackdata(StackData stackdata) {
        this.stackdata = stackdata;
    }
    /**
     * @return the stackdata
     */
    public StackData getStackdata() {
        return this.stackdata;
    }
    public BlockStack(int id) {
        this(id, 0, 1);
    }
    public BlockStack(Block block, int i) {
        this(block.id, 0, i);
    }
    public BlockStack(Block block) {
        this(block.id, 0, 1);
    }
    public BlockStack(int id, int data, int size) {
        this.id = id;
        this.data = data;
        this.size = size;
    }
    public BlockStack(int id, int data) {
        this(id, data, 1);
    }
    
    /**
     * 
     */
    public BlockStack() {
    }
    
    /**
     * @param t
     */
    public BlockStack(Tag.Compound t) {
        this.id = t.getInt("id");
        this.data = t.getInt("data");
        this.size = t.getInt("size");
        ByteArray b = t.getByteArray("stackdata");
        if (b != null) {
            try {
                StackData stackdata = new StackData();
                stackdata.read(ByteStreams.newDataInput(b.getArray()));
                this.stackdata = stackdata;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Tag save() {
        Tag.Compound tag = new Tag.Compound();
        tag.setInt("stacktype", 0);
        tag.setInt("id", this.id);
        tag.setInt("data", this.data);
        tag.setInt("size", this.size);
        if (this.stackdata != null) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            try {
                this.stackdata.write(out);
                tag.setByteArray("stackdata", out.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        return tag;
    }
    public BlockStack copy() {
        BlockStack stack = new BlockStack(this.id, this.data, this.size);
        if (this.stackdata != null)
            stack.stackdata = this.stackdata.copy();
        return stack;
    }
    public Block getBlock() {
        return Block.get(this.id);
    }
    @Override
    public void read(DataInput in) throws IOException {
        this.id = in.readInt();
        this.data = in.readInt();
        this.size = in.readInt();
        if (in.readByte()!=0) {
            this.stackdata = new StackData();
            this.stackdata.read(in);
        } else {
            this.stackdata = null;
        }
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeInt(this.data);
        out.writeInt(this.size);
        if (this.stackdata == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            this.stackdata.write(out);
        }
    }

    public boolean isEqualId(BaseStack s) {
        if (!(s instanceof BlockStack))
            return false;
        BlockStack other = (BlockStack)s;
        return this.id == other.id && this.data == other.data;
    }
    public boolean isFullyEqual(BaseStack s) {
        if (!(s instanceof BlockStack))
            return false;
        BlockStack other = (BlockStack)s;
        return this.id == other.id && this.data == other.data && this.size == other.size;
    }

    @Override
    public boolean isItem() {
        return false;
    }
    @Override
    public Item getItem() {
        return null;
    }
    
    @Override
    public String toString() {
        return "BlockStack[id="+this.id+",data="+this.data+",size="+this.size+",stackdata="+(this.stackdata!=null?this.stackdata.hashCode():"<null>")+"]";
    }
    
    /** Only for crafting calc purposes, does not reflect state of dynamic stackdata instances */
    @Override
    public int getTypeHash() {
        return (this.stackdata!=null?2:1)|((this.id<<16|this.data)<<2);
    }
}
