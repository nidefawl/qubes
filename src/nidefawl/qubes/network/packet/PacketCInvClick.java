package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.Handler;

public class PacketCInvClick extends Packet {
    public int       id;
    public int       idx;
    public int       button;
    public int       action;
    public BaseStack stack;
    public PacketCInvClick() {
    }

    public PacketCInvClick(int id, int idx, int button, int action, BaseStack stack) {
        super();
        this.id = id;
        this.idx = idx;
        this.button = button;
        this.action = action;
        this.stack = stack;
    }


    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.id = stream.readInt();
        this.idx = stream.readInt();
        this.button = stream.readInt();
        this.action = stream.readInt();
        this.stack = readStack(stream);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.id);
        stream.writeInt(this.idx);
        stream.writeInt(this.button);
        stream.writeInt(this.action);
        writeStack(this.stack, stream);
    }

    @Override
    public void handle(Handler h) {
        h.handleInvClick(this);
    }

}
