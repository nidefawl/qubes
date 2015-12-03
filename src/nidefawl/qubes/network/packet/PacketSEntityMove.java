/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketSEntityMove extends Packet {
    public int entId;
    public Vec3D pos;
    public float yaw, pitch;
    public int flags;
    public float yawBodyOffset;

    public PacketSEntityMove() {
    }
    public PacketSEntityMove(int entId, int flags, Vec3D pos, float pitch, float yaw, float yawBodyOffset) {
        this.entId = entId;
        this.flags = flags;
        this.pos = (flags & 1) != 0 ? new Vec3D(pos) : null;
        this.pitch = pitch;
        this.yaw = yaw;
        this.yawBodyOffset = yawBodyOffset;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.flags = stream.readUnsignedByte();
        this.entId = stream.readInt();
        if ((this.flags & 1) != 0)
            pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
        if ((this.flags & 2) != 0) {
            this.pitch = stream.readFloat();
            this.yaw = stream.readFloat();
            this.yawBodyOffset = stream.readFloat();
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeByte(this.flags);
        stream.writeInt(this.entId);
        if ((this.flags & 1) != 0) {
            stream.writeDouble(this.pos.x);
            stream.writeDouble(this.pos.y);
            stream.writeDouble(this.pos.z);
        }
        if ((this.flags & 2) != 0) {
            stream.writeFloat(this.pitch);
            stream.writeFloat(this.yaw);
            stream.writeFloat(this.yawBodyOffset);
        }
    }

    @Override
    public int getID() {
        return 24;
    }

    @Override
    public void handle(Handler h) {
        h.handleEntityMove(this);
    }

}
