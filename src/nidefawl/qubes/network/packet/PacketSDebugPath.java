package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.path.PathPoint;

public class PacketSDebugPath extends Packet {
    
    public List<PathPoint> pts;
    public PacketSDebugPath() {
    }
    public PacketSDebugPath(List<PathPoint> list) {
        this.pts = list;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        int n = stream.readInt();
        pts = Lists.newArrayListWithCapacity(n);
        for (int i = 0; i < n; i++) {
            int x = stream.readInt();
            int y = stream.readInt();
            int z = stream.readInt();
            pts.add(new PathPoint(x, y, z));
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        int n = this.pts.size();
        stream.writeInt(n);
        for (int i = 0; i < n; i++) {
            stream.writeInt(this.pts.get(i).x);
            stream.writeInt(this.pts.get(i).y);
            stream.writeInt(this.pts.get(i).z);
        }
    }

    @Override
    public void handle(Handler h) {
        h.handleDebugPath(this);
    }

}
