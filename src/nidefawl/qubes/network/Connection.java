package nidefawl.qubes.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketDisconnect;

public class Connection {

    public final Socket           socket;
    public final DataInputStream  inStream;
    public final DataOutputStream outStream;
    private final ReaderThread    readThread;
    private final WriterThread    writeThread;
    public volatile boolean       isConnected = true;
    private boolean               cleanUp     = false;
    LinkedBlockingQueue<Packet>   incoming    = new LinkedBlockingQueue<Packet>();
    LinkedBlockingQueue<Packet>   incoming2    = new LinkedBlockingQueue<Packet>();
    LinkedBlockingQueue<Packet>   outgoing    = new LinkedBlockingQueue<Packet>();
    private IHandler              handler;
	private InputStream sIn;

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

    public boolean readPackets() throws IOException {
    	try {

            final Packet p = Packet.read(this.inStream);
            if (p != null) {
                if (p.handleSynchronized())
                    this.incoming.add(p);
                else p.handle(this.handler);
                return true;
            } else {
                System.out.println("NOTHING!");
            }
    	} catch (Exception e) {
    		throw e;
    	}
        return false;
    }

    public boolean writePackets() throws InterruptedException, IOException {
        Packet p = this.outgoing.take();
        if (p != null) {
            Packet.write(p, this.outStream);
            while ((p = this.outgoing.peek()) != null) {
                Packet.write(p, this.outStream);
            }
            this.outStream.flush();
            return true;
        }
        return false;
    }

    public void onError(final Exception e) {
    	if (isConnected && !(e instanceof SocketException)) {
            e.printStackTrace();
    	}
        this.isConnected = false;
    }

    public void sendPacket(final Packet p) {
        if (this.isConnected) {
            this.outgoing.add(p);
        }
    }

    public void update() {
        if (!this.isConnected) {
            this.onDisconnect();
        }
        if (this.isConnected) {
            int max = 1000;
            while (max-- > 0) {
                final Packet p = this.incoming.poll();
                if (p != null) {
                    p.handle(this.handler);
                    continue;
                }
                break;
            }
            if (this.isConnected) {
                this.handler.update();
            }
        }
    }
    public void updateBlocking() throws InterruptedException {
        if (this.isConnected) {
            final Packet p = this.incoming2.take();
            if (p != null) {
                p.handle(this.handler);
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
                e.printStackTrace();
            }
            try {
                this.outStream.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            try {
                this.socket.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void interruptThreads() {
        this.readThread.interruptThread();
        this.writeThread.interruptThread();
    }

    public void disconnect(String reason) {
        if (this.isConnected) {
        	try {
        		interruptThreads();
                Packet.write(new PacketDisconnect(0, reason), this.outStream);
        		this.outStream.flush();
        		this.socket.shutdownOutput();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	
        	
            this.isConnected = false;
            this.onDisconnect();
            System.out.println(this.handler.getHandlerName()+" disconnected: "+reason);   
        }
    }

    public void setHandler(final IHandler handler) {
        this.handler = handler;
    }

    public boolean finished() {
        return this.cleanUp;
    }

	public InetSocketAddress getAddr() {
		return (InetSocketAddress) this.socket.getRemoteSocketAddress();
	}

    public IHandler getHandler() {
        return this.handler;
    }

    public void onFinish() {
        this.handler.onFinish();
    }

}
