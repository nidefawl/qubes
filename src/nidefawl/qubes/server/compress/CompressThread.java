package nidefawl.qubes.server.compress;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.Deflater;

import nidefawl.qubes.server.GameServer;

public class CompressThread implements Runnable {
    static Thread                                  thread;
    static final LinkedBlockingQueue<ICompressTask> queue = new LinkedBlockingQueue<>();
    private GameServer                             server;

    public CompressThread(GameServer gameServer) {
        this.server = gameServer;
    }

    public void run() {

        final Deflater deflate = new Deflater(this.server.getConfig().chunkCompressionLevel);
        final int i20Meg = 20*1024*1024;
        final byte[] tmpBuffer = new byte[i20Meg];
        final byte[] tmpBuffer2 = new byte[i20Meg];
        while (server.isRunning()) {
            try {
                ICompressTask task = queue.take();
                if (task != null) {
                    int len = task.fill(tmpBuffer);
                    deflate.reset();
                    deflate.setInput(tmpBuffer, 0, len);
                    deflate.finish();
                    int lenOut = deflate.deflate(tmpBuffer2);
                    byte[] compressed = new byte[lenOut];
                    System.arraycopy(tmpBuffer2, 0, compressed, 0, lenOut);
                    task.finish(compressed);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void startNewThread(GameServer gameServer) {
        thread = new Thread(new CompressThread(gameServer));
        thread.setDaemon(true);
        thread.setName("PacketCompressThread");
        thread.start();
    }

    public static void submit(ICompressTask task) {
        queue.offer(task);
    }
}
