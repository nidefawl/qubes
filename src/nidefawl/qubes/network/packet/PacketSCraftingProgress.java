package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.Handler;

public class PacketSCraftingProgress extends Packet {
    public int       id;
    public int       action;
    public long currentTime;
    public long startTime;
    public long endTime;
    public int recipe;
    public PacketSCraftingProgress() {
    }

    public PacketSCraftingProgress(int id, int idx, int button, int action, BaseStack stack) {
        super();
        this.id = id;
        this.action = action;
    }


    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.id = stream.readInt();
        this.action = stream.readInt();
        this.recipe = stream.readInt();
        this.currentTime = stream.readLong();
        this.startTime = stream.readLong();
        this.endTime = stream.readLong();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.id);
        stream.writeInt(this.action);
        stream.writeInt(this.recipe);
        stream.writeLong(this.currentTime);
        stream.writeLong(this.startTime);
        stream.writeLong(this.endTime);
    }

    @Override
    public void handle(Handler h) {
        h.handleCraftingProgress(this);
    }

}
