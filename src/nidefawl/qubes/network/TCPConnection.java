package nidefawl.qubes.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

import nidefawl.qubes.network.packet.InvalidPacketException;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketDisconnect;

public class TCPConnection extends Connection {
    public final Socket           socket;
    public final DataInputStream  inStream;
    public final DataOutputStream outStream;
    private final ReaderThread    readThread;
    private final WriterThread    writeThread;
    public TCPConnection(final Socket s) throws IOException {
        this.inStream = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        this.outStream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        this.readThread = new ReaderThread(this);
        this.writeThread = new WriterThread(this);
        this.socket = s;
    }

    public void startThreads() {
        this.readThread.start();
        this.writeThread.start();
        //        this.handler.startThread();
    }


    public boolean readPackets() throws IOException, InvalidPacketException {
        final Packet p = Packet.read(this.inStream);
        if (p != null) {
            this.incoming.add(p);
            return true;
        } else {
            System.out.println("NOTHING!");
        }
        return false;
    }

    public boolean writePackets() throws InterruptedException, IOException {
        Packet p = this.outgoing.take();
        if (p != null) {
            Packet.write(p, this.outStream);
            while ((p = this.outgoing.poll()) != null) {
                Packet.write(p, this.outStream);
            }
            this.outStream.flush();
            return true;
        }
        return false;
    }
    protected void immediateDisconnect(String reason) throws IOException {
        Packet.write(new PacketDisconnect(0, reason), this.outStream);
        this.outStream.flush();
        this.socket.shutdownOutput();
    }

    public InetSocketAddress getAddr() {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
    }

    public String getHostnameString() {
        return getAddr().getHostString();
    }
    
    protected void closeSocket() {
        try {
            this.inStream.close();
        } catch (final Exception e) {
        }
        try {
            this.outStream.close();
        } catch (final Exception e) {
        }
        try {
            this.socket.close();
        } catch (final Exception e) {
        }
    }
    
    public void interruptThreads() {
        this.readThread.interruptThread();
        this.writeThread.interruptThread();
    }

    @Override
    public int getCompression() {
        return 1;
    }

}
