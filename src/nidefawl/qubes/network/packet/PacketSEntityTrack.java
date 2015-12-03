/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.TagReadLimiter;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketSEntityTrack extends Packet {

    public int entId;
    public int entType;
    public Vec3D pos;
    public float yaw, yawbody, pitch;
    public Tag data;

    /**
     * 
     */
    public PacketSEntityTrack() {
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        int flags = stream.readUnsignedByte();
        this.entId = stream.readInt();
        this.entType = stream.readInt();
        pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
        this.yaw = stream.readFloat();
        this.yawbody = stream.readFloat();
        this.pitch = stream.readFloat();
        if ((flags & 0x1) != 0) {
            this.data = Tag.read(stream, TagReadLimiter.UNLIMITED);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        int flags = 0;
        if (this.data != null) {
            flags |= 1;
        }
        stream.writeByte(flags);
        stream.writeInt(this.entId);
        stream.writeInt(this.entType);
        stream.writeDouble(this.pos.x);
        stream.writeDouble(this.pos.y);
        stream.writeDouble(this.pos.z);
        stream.writeFloat(this.yaw);
        stream.writeFloat(this.yawbody);
        stream.writeFloat(this.pitch);
        if (this.data != null) {
            Tag.write(data, stream);
        }
        
    }

    @Override
    public int getID() {
        return 22;
    }

    @Override
    public void handle(Handler h) {
        h.handleEntityTrack(this);
    }

}
