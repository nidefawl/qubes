package nidefawl.qubes.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.StartServer;
import nidefawl.qubes.network.packet.InvalidPacketException;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketDisconnect;
import nidefawl.qubes.util.GameContext;

public class Connection {
    public final static int REMOTE = 0;
    public final static int LOCAL  = 1;

    public final Socket           socket;
    public final DataInputStream  inStream;
    public final DataOutputStream outStream;
    private final ReaderThread    readThread;
    private final WriterThread    writeThread;
    public volatile boolean       isConnected      = true;
    private boolean               cleanUp          = false;
    LinkedBlockingQueue<Packet>   incoming         = new LinkedBlockingQueue<Packet>();
    LinkedBlockingQueue<Packet>   outgoing         = new LinkedBlockingQueue<Packet>();
    private InputStream           sIn;
    private Throwable             readWriteException;
    private int                   disconnectFrom   = -1;
    private String                disconnectReason = null;

    public Connection(final Socket s) throws IOException {
        this.socket = s;
        this.sIn = s.getInputStream();
        this.inStream = new DataInputStream(new BufferedInputStream(this.sIn));
        this.outStream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        this.readThread = new ReaderThread(this);
        this.writeThread = new WriterThread(this);
    }

    public void startThreads() {
        this.readThread.start();
        this.writeThread.start();
        //        this.handler.startThread();
    }

    public boolean isConnected() {
        return this.isConnected;
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
                    this.disconnect(REMOTE, "Connection closed");

                } else {
                    this.disconnect(LOCAL, "Error: " + excMessage);
                }
                if (!(this.readWriteException instanceof IOException)) {
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
    }

    public void interruptThreads() {
        this.readThread.interruptThread();
        this.writeThread.interruptThread();
    }

    public void disconnect(int from, String reason) {
        if (this.isConnected) {
            if (Thread.currentThread() != GameContext.getMainThread()) {
                System.err.println("Disconnect from non-mainthread: " + Thread.currentThread() + " (Mainthread: " + GameContext.getMainThread() + ")");
            }
            try {
                this.interruptThreads();
                if (from != Connection.REMOTE) {
                    Packet.write(new PacketDisconnect(0, reason), this.outStream);
                    this.outStream.flush();
                    this.socket.shutdownOutput();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
            }
            this.disconnectReason = reason;
            this.disconnectFrom = from;
            this.isConnected = false;
            this.onDisconnect();
        }
    }

    public boolean finished() {
        return this.cleanUp;
    }

    public InetSocketAddress getAddr() {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
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
}
