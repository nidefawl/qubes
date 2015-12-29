package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;

public class PacketCSetBlock extends AbstractPacketWorldRef {
    public PacketCSetBlock() {
    }
    public final BlockPos pos = new BlockPos();
    public final Vector3f fpos = new Vector3f();
    public int face;
    public BlockStack stack;

    public PacketCSetBlock(int id, BlockPos blockPos, int face, BlockStack stack) {
        this(id, blockPos, Vector3f.ZERO, face, stack);
    }
    public PacketCSetBlock(int id, BlockPos blockPos, Vector3f pos, int face, BlockStack stack) {
        super(id);
        this.pos.set(blockPos);
        this.fpos.set(pos);
        this.face = face;
        this.stack = stack;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.pos.read(stream);
        this.fpos.read(stream);
        face = stream.readInt();
        stack = new BlockStack();
        stack.read(stream);

    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        this.pos.write(stream);
        this.fpos.write(stream);
        stream.writeInt(face);
        stack.write(stream);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleSetBlock(this);    
        }   
    }

}
