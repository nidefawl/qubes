package nidefawl.qubes.config;

import java.io.File;
import nidefawl.qubes.network.ReaderThread;
import nidefawl.qubes.network.WriterThread;
import nidefawl.qubes.network.server.NetworkServer;

public class GameServer implements Runnable {
	final ServerConfig config = new ServerConfig();
	Thread mainThread;
	Thread handshakeThread;
	NetworkServer networkServer;
	private boolean running;
	private boolean finished;

	public GameServer() {

	}

	public void startServer() {
		try {
			this.networkServer = new NetworkServer(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.mainThread = new Thread(this);
		this.mainThread.setPriority(Thread.MAX_PRIORITY);
		this.mainThread.start();
	}

	@Override
	public void run() {
		try {
			this.running = true;
			networkServer.startListener();
			System.out.println("server is running");
			while (this.running) {
				loop();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("server ended");
			this.finished = true;
		}
	}

	protected void loop() {
		try {
			this.networkServer.update();
			System.out.println("Writer threads: "+WriterThread.ACTIVE_THREADS);
			System.out.println("Reader threads: "+ReaderThread.ACTIVE_THREADS);
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void halt() {
		if (this.running) {
			this.running = false;
			System.out.println("Shutting down server...");
			try {
				this.networkServer.halt();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void loadConfig() {
		try {
			this.config.load(new File("config", "config.yml"));
		} catch (InvalidConfigException e) {
			e.printStackTrace();
		}
	}

	public ServerConfig getConfig() {
		return this.config;
	}

}
