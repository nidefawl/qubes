package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

public class PacketSTeleport extends AbstractPacketWorldRef {
    public Vec3D pos;
    public float yaw;
    public float pitch;
    public int flags;
    public int sync;

    public PacketSTeleport() {
    }

    public PacketSTeleport(int id, int sync, Vec3D pos, float yaw, float pitch, int flags) {
        super(id);
        this.sync = sync;
        this.pos = new Vec3D(pos);
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.sync = stream.readInt();
        this.flags = stream.readInt();
        this.pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
        this.yaw = stream.readFloat();
        this.pitch = stream.readFloat();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(sync);
        stream.writeInt(flags);
        stream.writeDouble(this.pos.x);
        stream.writeDouble(this.pos.y);
        stream.writeDouble(this.pos.z);
        stream.writeFloat(yaw);
        stream.writeFloat(pitch);
    }



    @Override
    public void handle(Handler h) {
        h.handleTeleport(this);
    }

}
