package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;

public class PacketCBlockAction extends AbstractPacketWorldRef {
    public static final int SET_BLOCK = 0;
    public static final int JUMP = 1;

    public PacketCBlockAction() {
    }
    public final BlockPos pos = new BlockPos();
    public final Vector3f fpos = new Vector3f();
    public int face;
    public BlockStack stack;
    public int action;
    public int selBlock;

    public PacketCBlockAction(int id, BlockPos blockPos, int face, BlockStack stack) {
        this(id, blockPos, Vector3f.ZERO, face, stack, SET_BLOCK);
    }
    public PacketCBlockAction(int id, BlockPos blockPos, Vector3f pos, int face, BlockStack stack, int action) {
        super(id);
        this.pos.set(blockPos);
        this.fpos.set(pos);
        this.face = face;
        this.stack = stack;
        this.action = action;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.pos.read(stream);
        this.fpos.read(stream);
        this.face = stream.readInt();
        this.stack = new BlockStack();
        this.stack.read(stream);
        this.action = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        this.pos.write(stream);
        this.fpos.write(stream);
        stream.writeInt(this.face);
        this.stack.write(stream);
        stream.writeInt(this.action);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleBlockAction(this);    
        }   
    }

}
