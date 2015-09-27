package nidefawl.qubes.world;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.network.packet.Packet;

public class WorldSettingsClient implements IWorldSettings {
    UUID           uuid;
    long           seed;
    long           time;
    private int    id;
    private String worldName;
    long           dayLen;
    boolean        isFixedTime;

    @Override
    public void read(DataInput in) throws IOException {
        this.id = in.readInt();
        this.uuid = new UUID(in.readLong(), in.readLong());
        this.seed = in.readLong();
        this.time = in.readLong();
        this.dayLen = in.readLong();
        this.isFixedTime = in.readByte() != 0;
        this.worldName = Packet.readString(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        throw new UnsupportedOperationException("This method should not be called");
    }

    public WorldSettingsClient() {

    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.worldName;
    }

    @Override
    public long getDayLen() {
        return this.dayLen;
    }

    @Override
    public boolean isFixedTime() {
        return this.isFixedTime;
    }

    @Override
    public void setTime(long l) {
        this.time = l;
    }

    @Override
    public void setFixedTime(boolean b) {
        this.isFixedTime = b;
    }

    @Override
    public void setDayLen(long dayLen) {
        this.dayLen = dayLen;
    }

}
