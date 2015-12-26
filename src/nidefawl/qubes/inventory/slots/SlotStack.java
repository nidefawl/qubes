package nidefawl.qubes.inventory.slots;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.network.packet.Packet;

public class SlotStack implements StreamIO {
    public SlotStack() {
        
    }
    public SlotStack(int slot, BaseStack stack) {
        this.slot = slot;
        this.stack = stack;
    }
    public int slot;
    public BaseStack stack;
    @Override
    public void read(DataInput in) throws IOException {
        this.slot = in.readUnsignedByte();
        this.stack = Packet.readStack(in);
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(this.slot);
        Packet.writeStack(this.stack, out);
    }
}
