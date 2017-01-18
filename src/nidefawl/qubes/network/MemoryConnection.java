package nidefawl.qubes.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import nidefawl.qubes.network.packet.InvalidPacketException;
import nidefawl.qubes.network.packet.Packet;

public class MemoryConnection extends Connection {

    private boolean isClient;
    MemoryConnection otherEnd;

    public MemoryConnection(boolean isClient) throws IOException {
        this.isClient = isClient;
    }
    
    public void setOtherEnd(MemoryConnection otherEnd) {
        this.otherEnd = otherEnd;
    }
    
    @Override
    public String getHostnameString() {
        return "MemoryConnection";
    }

    @Override
    public void sendPacket(Packet p) {
        this.otherEnd.incoming.add(p);
    }

    @Override
    public boolean readPackets() throws IOException, InvalidPacketException {
        return false;
    }

    @Override
    public boolean writePackets() throws InterruptedException, IOException {
        return false;
    }

    @Override
    public int getCompression() {
        return 0;
    }
}
