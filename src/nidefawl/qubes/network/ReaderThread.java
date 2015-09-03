package nidefawl.qubes.network;

public class ReaderThread extends Thread {
	public static int ACTIVE_THREADS = 0;

	private final Connection conn;
	public volatile boolean interrupted = false;

	public ReaderThread(final Connection connection) {
		this.setName("ReadThread");
		this.setDaemon(true);
		this.conn = connection;
	}

	public void interruptThread() {
		if (!this.interrupted) {
			this.interrupted = true;
			this.interrupt();
		}
	}

	@Override
	public void run() {
		ACTIVE_THREADS++;
		try {

			while (this.conn.isConnected()) {
				try {
					while (this.conn.readPackets())
						;
					if (!this.interrupted) {
						Thread.sleep(100);
					}
					if (this.interrupted) {
						Thread.interrupted();
						this.interrupted = false;
					}
				} catch (final Exception e) {
					this.conn.onError(e);
				}
			}
		} finally {
			ACTIVE_THREADS--;
		}
	}
}
