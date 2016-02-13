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
public class PacketSEntityProperties extends Packet {

    public int entId;
    public Tag data;

    public PacketSEntityProperties() {
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.entId = stream.readInt();
        this.data = Tag.read(stream, TagReadLimiter.UNLIMITED);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.entId);
        Tag.write(data, stream);
    }



    @Override
    public void handle(Handler h) {
        h.handleEntityProperties(this);
    }

}
