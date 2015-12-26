package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

public class PacketCSwitchWorld extends Packet {
    public int flags;

    public PacketCSwitchWorld() {
    }

    public PacketCSwitchWorld(int flags) {
        this.flags = flags;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.flags = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(flags);
    }



    @Override
    public void handle(Handler h) {
        h.handleSwitchWorld(this);
    }

}
