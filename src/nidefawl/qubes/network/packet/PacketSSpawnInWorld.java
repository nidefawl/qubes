package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;

public class PacketSSpawnInWorld extends Packet {
    public int id;
    public Vec3D pos;
    public UUID  uuid;
    public long  seed;
    public int   time;
    public int flags;
    public String worldName;

    public PacketSSpawnInWorld() {
    }

    public PacketSSpawnInWorld(int id, Vec3D pos, int flags, UUID uuid, String name, long seed, int time) {
        this.id = id;
        this.pos = new Vec3D(pos);
        this.uuid = uuid;
        this.seed = seed;
        this.time = time;
        this.flags = flags;
        this.worldName = name;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.id = stream.readInt();
        pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
        uuid = new UUID(stream.readLong(), stream.readLong());
        worldName = readString(stream);
        seed = stream.readLong();
        time = stream.readInt();
        flags = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.id);
        stream.writeDouble(this.pos.x);
        stream.writeDouble(this.pos.y);
        stream.writeDouble(this.pos.z);
        stream.writeLong(this.uuid.getMostSignificantBits());
        stream.writeLong(this.uuid.getLeastSignificantBits());
        writeString(this.worldName, stream);
        stream.writeLong(this.seed);
        stream.writeInt(this.time);
        stream.writeInt(this.flags);
    }

    @Override
    public int getID() {
        return 5;
    }

    @Override
    public void handle(Handler h) {
        h.handleSpawnInWorld(this);
    }

}
