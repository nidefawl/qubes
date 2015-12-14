/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Stack;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.ByteArray;
import nidefawl.qubes.network.StreamIO;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockStack extends BaseStack {

    public int id;
    public int data;
    public int size;
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
        tag.setInt("type", 0);
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
    /**
     * @return
     */
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

    public boolean isEqualId(BlockStack s) {
        return this.id == s.id && this.data == s.data;
    }
    public boolean isFullyEqual(BlockStack s) {
        return this.id == s.id && this.data == s.data && this.size == s.size;
    }

    @Override
    public boolean isItem() {
        return false;
    }
}
