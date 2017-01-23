package nidefawl.qubes.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.network.packet.InvalidPacketException;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.util.GameContext;

public abstract class Connection {
    public final static int REMOTE = 0;
    public final static int LOCAL  = 1;

    public volatile boolean       isConnected      = true;
    private boolean               cleanUp          = false;
    LinkedBlockingQueue<Packet>   incoming         = new LinkedBlockingQueue<Packet>();
    LinkedBlockingQueue<Packet>   outgoing         = new LinkedBlockingQueue<Packet>();
    private Throwable             readWriteException;
    private int                   disconnectFrom   = -1;
    private String                disconnectReason = null;

    public Connection() throws IOException {
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public abstract boolean readPackets() throws IOException, InvalidPacketException;

    public abstract boolean writePackets() throws InterruptedException, IOException;

    public void onError(final Exception e) {
        // TODO: check if we always catch the correct error 
        //(not eof/readtimeout after a NPE in a packet)
        if (this.readWriteException == null) {
            this.readWriteException = e;
        }
    }

    public void sendPacket(final Packet p) {
        if (this.isConnected) {
            this.outgoing.add(p);
        }
    }

    public void validateConnection() {
        if (!this.isConnected) {
            this.onDisconnect();
        } else {
            if (this.readWriteException != null) {
                String excMessage = this.readWriteException.getMessage();
                if (this.readWriteException instanceof EOFException) {
                    this.disconnect(LOCAL, "Connection closed");

                } else {
                    this.disconnect(LOCAL, "Error: " + excMessage);
                }
                if (this.readWriteException instanceof InvalidPacketException) {
                    this.readWriteException.printStackTrace();
                } else if (!(this.readWriteException instanceof IOException)) {
                    this.readWriteException.printStackTrace();
                } else if (this.readWriteException instanceof RuntimeException || this.readWriteException.getCause() instanceof RuntimeException) {
                    this.readWriteException.printStackTrace();
                }
            }
        }
    }

    private void onDisconnect() {
        if (!this.cleanUp) {

            this.cleanUp = true;
            this.interruptThreads();
            closeSocket();

        }
    }

    protected void closeSocket() {
    }

    public void interruptThreads() {
    }

    public void disconnect(int from, String reason) {
        if (this.disconnectReason == null || (from == Connection.REMOTE && this.disconnectFrom == Connection.LOCAL)) {
            this.disconnectReason = reason;
            this.disconnectFrom = from;
        }
        if (this.isConnected) {
            if (Thread.currentThread() != GameContext.getMainThread()) {
                System.err.println("Disconnect from non-mainthread: " + Thread.currentThread() + " (Mainthread: " + GameContext.getMainThread() + ")");
            }
            try {
                this.interruptThreads();
                if (from != Connection.REMOTE) {
                    immediateDisconnect(reason);
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
            }
            this.isConnected = false;
            this.onDisconnect();
        }
    }

    protected void immediateDisconnect(String reason) throws IOException {
    }

    public boolean finished() {
        return this.cleanUp;
    }

    public Packet pollPacket() {
        return this.incoming.poll();
    }

    public LinkedBlockingQueue<Packet> getIncoming() {
        return this.incoming;
    }

    public int getDisconnectFrom() {
        return disconnectFrom;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public InetSocketAddress getAddr() {
        return null;
    }

    public abstract String getHostnameString();

    public void startThreads() {
    }

    public abstract int getCompression();
}
