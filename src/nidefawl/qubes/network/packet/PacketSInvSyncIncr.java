package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.network.Handler;

public class PacketSInvSyncIncr extends Packet {

    public int invId;
    public Collection<SlotStack> stacks;
    public PacketSInvSyncIncr(int id, Collection<SlotStack> stacks) {
        this.invId = id;
        this.stacks = stacks;
    }
    public PacketSInvSyncIncr() {
    }
    

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.invId = stream.readInt();
        int len = stream.readInt();
        this.stacks = Lists.newArrayListWithCapacity(0);
        for (int i = 0; i < len; i++) {
            SlotStack stack = new SlotStack();
            stack.read(stream);
            this.stacks.add(stack);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.invId);
        stream.writeInt(this.stacks.size());
        for (SlotStack stack : this.stacks) {
            stack.write(stream);
        }
    }

    @Override
    public void handle(Handler h) {
        h.handleInvSyncIncr(this);
    }

}
