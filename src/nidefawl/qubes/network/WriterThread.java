package nidefawl.qubes.network;

public class WriterThread extends Thread {
	public static int ACTIVE_THREADS = 0;
    private final Connection conn;
    public volatile boolean  interrupted = false;

    public WriterThread(final Connection connection) {
        this.setName("WriteThread");
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
                    this.conn.writePackets();
                    if (this.interrupted) {
                        Thread.interrupted();
                        this.interrupted = false;
                        break;
                    }
                } catch (final Exception e) {
                    if (this.interrupted) {
                        Thread.interrupted();
                        this.interrupted = false;
                        break;
                    } else
                    	this.conn.onError(e);
                }
            }
    	} finally {
    		ACTIVE_THREADS--;
    	}
    }
}
