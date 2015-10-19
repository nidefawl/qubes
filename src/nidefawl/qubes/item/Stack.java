/**
 * 
 */
package nidefawl.qubes.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.network.StreamIO;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Stack implements StreamIO {

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
    public Stack(int id) {
        this(id, 0, 1);
    }
    public Stack(Stack stack) {
        this(stack.id, stack.data, stack.size);
        if (stack.stackdata != null) {
            this.stackdata = stack.stackdata.copy();
        }
    }
    public Stack(int id, int data, int size) {
        this.id = id;
        this.data = data;
        this.size = size;
    }
    public Stack(int id, int data) {
        this(id, data, 1);
    }
    
    /**
     * 
     */
    public Stack() {
    }
    
    public Stack copy() {
        return new Stack(this);
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
    /**
     * @param selBlock
     * @return
     */
    public boolean isEqualId(Stack s) {
        return this.id == s.id && this.data == s.data;
    }
    public boolean isFullyEqual(Stack s) {
        return this.id == s.id && this.data == s.data && this.size == s.size;
    }
    
    

}
