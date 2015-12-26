/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketSEntityUnTrack extends Packet {

    /**
     * 
     */
    public PacketSEntityUnTrack() {
    }
    public int entId;
    public PacketSEntityUnTrack(int id) {
        this.entId = id;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.entId = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.entId);
    }



    @Override
    public void handle(Handler h) {
        h.handleEntityUntrack(this);
    }

}
