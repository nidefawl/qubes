/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.Handler;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketSEntityEquip extends Packet {
    public int entId;;
    public BaseStack[] stacks;

    public PacketSEntityEquip() {
    }
    public PacketSEntityEquip(int entId, BaseStack[] equipment) {
        this.entId = entId;
        this.stacks = equipment;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.entId = stream.readInt();
        int len = stream.readInt();
        this.stacks = new BaseStack[len];
        for (int i = 0; i < len; i++) {
            this.stacks[i] = Packet.readStack(stream);
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.entId);
        stream.writeInt(this.stacks.length);
        for (int i = 0; i < this.stacks.length; i++) {
            Packet.writeStack(this.stacks[i], stream);
        }
    }



    @Override
    public void handle(Handler h) {
        h.handleEntityEquip(this);
    }

}
