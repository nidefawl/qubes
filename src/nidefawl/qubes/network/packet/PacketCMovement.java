package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

public class PacketCMovement extends Packet {
    public Vec3D pos;
    public int flags;

    public PacketCMovement() {
    }

    public PacketCMovement(Vec3D pos, int flags) {
        this.pos = new Vec3D(pos);
        this.flags = flags;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.flags = stream.readInt();
        pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(flags);
        stream.writeDouble(this.pos.x);
        stream.writeDouble(this.pos.y);
        stream.writeDouble(this.pos.z);
    }

    @Override
    public int getID() {
        return 6;
    }

    @Override
    public void handle(Handler h) {
        h.handleMovement(this);
    }

}
