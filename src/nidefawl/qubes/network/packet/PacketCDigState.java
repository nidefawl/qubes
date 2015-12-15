package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;

public class PacketCDigState extends AbstractPacketWorldRef {
    
    public PacketCDigState() {
    }

    public final BlockPos pos = new BlockPos();
    public final Vector3f fpos = new Vector3f();
    public int face;
    public BaseStack stack;
    public int stage;
    
    public PacketCDigState(int worldid, int stage, BlockPos pos, Vector3f fpos, int faceHit, BaseStack stack) {
        super(worldid);
        this.stage = stage;
        this.pos.set(pos);
        this.fpos.set(fpos);
        this.face = faceHit;
        this.stack = stack;
    }
    
    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.stage = stream.readInt();
        this.face = stream.readInt();
        this.pos.read(stream);
        this.fpos.read(stream);
        int s = stream.readByte();
        if (s == 1) {
            this.stack = new ItemStack();
            this.stack.read(stream);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.stage);
        stream.writeInt(this.face);
        this.pos.write(stream);
        this.fpos.write(stream);
        if (this.stack != null) {
            stream.writeByte(1);
            this.stack.write(stream);
        } else {
            stream.writeByte(0);
        }
    }

    @Override
    public int getID() {
        return 26;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
            h.handleDigState(this);
    }

}
