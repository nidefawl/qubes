/**
 * 
 */
package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Lists;

import nidefawl.qubes.network.Handler;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class PacketChatChannels extends Packet {


    public ArrayList<String> list;
    
    /**
     * 
     */
    public PacketChatChannels() {
    }

    /**
     * @param joinedChannels
     */
    public PacketChatChannels(Collection<String> joinedChannels) {
        this.list = Lists.newArrayList(joinedChannels);
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        int len = stream.readInt();
        this.list = Lists.newArrayListWithCapacity(len);
        for (int i = 0; i < len; i++) {
            this.list.add(readString(stream, 32));
        }
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.network.packet.Packet#writePacket(java.io.DataOutput)
     */
    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.list.size());
        for (int i = 0; i < this.list.size(); i++) {
            writeString(this.list.get(i), stream);
        }
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.network.packet.Packet#getID()
     */


    /* (non-Javadoc)
     * @see nidefawl.qubes.network.packet.Packet#handle(nidefawl.qubes.network.Handler)
     */
    @Override
    public void handle(Handler h) {
        h.handleChannels(this);
    }

}
