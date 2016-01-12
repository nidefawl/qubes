package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.network.Handler;

public class PacketSInvCarried extends Packet {

    public SlotStack stack;
    public PacketSInvCarried(SlotStack stack) {
        this.stack = stack;
    }
    public PacketSInvCarried() {
    }
    

    @Override
    public void readPacket(DataInput stream) throws IOException {
        stack = new SlotStack();
        stack.read(stream);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stack.write(stream);
    }

    @Override
    public void handle(Handler h) {
        h.handleInvCarried(this);
    }

}
