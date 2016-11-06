package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.io.network.DataListType;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.network.StreamIO;

public class PacketSList extends Packet {

    public int reqId;
    public DataListType type;
    public List list;
    public PacketSList() {
    }

    public PacketSList(int reqId, DataListType type, List list) {
        this.reqId = reqId;
        this.type = type;
        this.list = list;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.reqId = stream.readInt();
        this.type = DataListType.byId(stream.readInt());
        int len = stream.readInt();
        this.list = Lists.newArrayListWithExpectedSize(len);
        for (int i = 0; i < len; i++) {
            StreamIO obj = this.type.makeNew();
            obj.read(stream);
            this.list.add(obj);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.reqId);
        stream.writeInt(this.type.getId());
        int len = this.list.size();
        stream.writeInt(len);
        for (int i = 0; i < len; i++) {
            ((StreamIO) list.get(i)).write(stream);
        }
    }

    @Override
    public void handle(Handler h) {
        h.handleList(this);
    }

}
