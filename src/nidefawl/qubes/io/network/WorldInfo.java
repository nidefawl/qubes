package nidefawl.qubes.io.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.network.packet.Packet;

public class WorldInfo implements StreamIO {
    public int id;
    public String name;
    public UUID uuid;
    public WorldInfo() {
    }
    @Override
    public void read(DataInput in) throws IOException {
        this.uuid = new UUID(in.readLong(), in.readLong());
        this.id = in.readInt();
        this.name = Packet.readString(in);
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.uuid.getMostSignificantBits());
        out.writeLong(this.uuid.getLeastSignificantBits());
        out.writeInt(this.id);
        Packet.writeString(this.name, out);
    }
}
