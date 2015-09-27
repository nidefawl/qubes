package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

public class PacketSWorldTime extends AbstractPacketWorldRef {
    public long   time;
    public long   daylen;
    public boolean isFixed;

    public PacketSWorldTime() {
    }

    public PacketSWorldTime(int id, long time, long daylen, boolean isFixed) {
        super(id);
        this.time = time;
        this.daylen = daylen;
        this.isFixed = isFixed;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.time = stream.readLong();
        this.daylen = stream.readLong();
        this.isFixed = stream.readByte() != 0;
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeLong(this.time);
        stream.writeLong(this.daylen);
        stream.writeByte(this.isFixed ? 1 : 0);
    }

    @Override
    public int getID() {
        return 18;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleWorldTime(this);   
        }
    }

}
