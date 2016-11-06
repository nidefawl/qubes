package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.io.network.DataListType;
import nidefawl.qubes.network.Handler;

public class PacketCListRequest extends Packet {

    public int reqId;
    public DataListType type;
    public PacketCListRequest() {
    }

    public PacketCListRequest(int reqId, DataListType type) {
        this.reqId = reqId;
        this.type = type;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.reqId = stream.readInt();
        this.type = DataListType.byId(stream.readInt());
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.reqId);
        stream.writeInt(this.type.getId());
    }

    @Override
    public void handle(Handler h) {
        h.handleListReq(this);
    }

}
