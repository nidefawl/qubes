package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.Handler;

public class PacketCCrafting extends Packet {
    public int  catid;
    public int  recipeid;
    public int  action;
    public int amount;
    public PacketCCrafting() {
    }

    public PacketCCrafting(int catid, int recipeid, int action) {
        this(catid, recipeid, action, 0);
    }
    public PacketCCrafting(int catid, int recipeid, int action, int n) {
        super();
        this.catid = catid;
        this.recipeid = recipeid;
        this.action = action;
        this.amount = n;
    }


    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.catid = stream.readInt();
        this.recipeid = stream.readInt();
        this.action = stream.readInt();
        this.amount = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.catid);
        stream.writeInt(this.recipeid);
        stream.writeInt(this.action);
        stream.writeInt(this.amount);
    }

    @Override
    public void handle(Handler h) {
        h.handleCrafting(this);
    }

}
