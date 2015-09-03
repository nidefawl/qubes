package nidefawl.qubes.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ListenThread extends Thread {
    private final ServerSocket serverSocket;
    private final NetworkServer       server;
	private boolean finished;

    public ListenThread(final NetworkServer server, final int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.server = server;
    }

    @Override
    public void run() {
    	try {
            while (this.server.isRunning) {
                try {
                    final Socket s = this.serverSocket.accept();
                    this.server.addConnection(s);
                } catch (final IOException ioe) {
                	if (this.server.isRunning) {
                        ioe.printStackTrace();	
                	}
                } catch (final Exception ioe) {
                    ioe.printStackTrace();
                }
            }
    	} finally {
    		this.finished = true;
    	}
    }
	public void halt() {
        if (!this.finished) {
            try {
            	this.serverSocket.close();
            } catch (Exception e) {
            }
            try {
                Thread.sleep(60);
            } catch (Exception e) {
            }
            while (!this.finished) {
                try {
                    this.interrupt();
                } catch (Exception e) {
                }
                try {
                    Thread.sleep(60);
                } catch (Exception e) {
                }
            }
        }
	}

}
