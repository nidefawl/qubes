package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.world.IWorldSettings;
import nidefawl.qubes.world.WorldSettingsClient;

public class PacketSSpawnInWorld extends Packet {
    public IWorldSettings worldSettings;
    public Vec3D          pos;
    public int            flags;
    public int            entId;

    public PacketSSpawnInWorld() {
    }

    public PacketSSpawnInWorld(int id, IWorldSettings worldSettings, Vec3D pos, int flags) {
        this.entId = id;
        this.worldSettings = worldSettings;
        this.pos = pos;
        this.flags = flags;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.entId = stream.readInt();
        this.flags = stream.readInt();
        pos = new Vec3D(stream.readDouble(), stream.readDouble(), stream.readDouble());
        worldSettings = new WorldSettingsClient();
        worldSettings.read(stream);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.entId);
        stream.writeInt(this.flags);
        stream.writeDouble(this.pos.x);
        stream.writeDouble(this.pos.y);
        stream.writeDouble(this.pos.z);
        this.worldSettings.write(stream);
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
