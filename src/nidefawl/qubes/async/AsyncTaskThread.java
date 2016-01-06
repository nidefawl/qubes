package nidefawl.qubes.async;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class AsyncTaskThread extends Thread {
    public AsyncTaskThread(Runnable r) {
        super(r);
    }

    //TODO: move decompression to thread
    final Inflater inflate = new Inflater();
    final int i10Meg = 20*1024*1024;

    byte[] tmpBuffer = new byte[i10Meg];
    public byte[] inflate(byte[] blocks) {
        inflate.reset();
        inflate.setInput(blocks);
        byte[] out = null;
        try {
            int len = inflate.inflate(tmpBuffer);
            out = new byte[len];
            System.arraycopy(tmpBuffer, 0, out, 0, len);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        return out;
    }
}
