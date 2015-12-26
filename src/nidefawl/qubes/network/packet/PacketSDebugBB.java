package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.AABB;

public class PacketSDebugBB extends Packet {
    
    public List<AABB> boxes;
    public PacketSDebugBB() {
    }
    public PacketSDebugBB(List<AABB> list) {
        this.boxes = list;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        int n = stream.readInt();
        boxes = Lists.newArrayListWithCapacity(n);
        for (int i = 0; i < n; i++) {
            AABB bb = new AABB();
            bb.read(stream);
            boxes.add(bb);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        int n = this.boxes.size();
        stream.writeInt(n);
        for (int i = 0; i < n; i++) {
            this.boxes.get(i).write(stream);
        }
    }



    @Override
    public void handle(Handler h) {
        h.handleDebugBBs(this);
    }

}
