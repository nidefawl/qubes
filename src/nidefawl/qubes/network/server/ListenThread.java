package nidefawl.qubes.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import nidefawl.qubes.config.ServerConfig;


public class ListenThread extends Thread {
    private final ServerSocket serverSocket;
    private final NetworkServer       server;
	private boolean finished;
	private boolean listening;

    public ListenThread(final NetworkServer server, final ServerConfig serverConfig) throws IOException {
        setName("ListenThread");
        setDaemon(true);
        int port = serverConfig.port;
        ServerSocket socket = null;
        if (port == 0) {
            IOException e2 = null;
            for (int i = 0; i < 10; i++) {
                port = 21087+i;
                try {
                    socket = new ServerSocket(port);
                    serverConfig.port = port;
                } catch (IOException e) {
                    e2 = e;
                }
            }
            if (socket == null) {
                throw e2;
            }
        } else {
            socket = new ServerSocket(port);
        }
        this.serverSocket = socket;
        this.server = server;
        this.finished = true;
        this.listening = false;
    }

    @Override
    public void run() {
    	try {
            this.finished = false;
    	    this.listening = true;
            while (this.listening && this.server.isRunning) {
                try {
                    final Socket s = this.serverSocket.accept();
                    System.out.println("serverSocket.accept ");
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
            try {
                this.serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    		this.finished = true;
    	}
    }
	public void halt() {
        listening = false;
        try {
            this.serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (!this.finished) {
            try {
                this.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

}
