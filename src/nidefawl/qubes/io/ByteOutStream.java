/**
 * 
 */
package nidefawl.qubes.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ByteOutStream extends OutputStream {

    private int       offset = 4;
    private final int pre    = 4;
    private byte      data[] = new byte[pre];

    int left() {
        return data.length - offset;
    }

    void realloc(int newLen) {
        byte[] newData = new byte[newLen];
        System.arraycopy(data, 0, newData, 0, offset);
        this.data = newData;
    }

    /**
     * 
     */
    private void checkRealloc(int i) {
        if (left() < i) {
            realloc((data.length) + i + 1024);
        }
    }

    public void write(int b) {
        checkRealloc(1);
        data[offset++] = (byte) b;
    }
    
    public void write(byte b[], int off, int len) throws IOException {
        checkRealloc(len);
        System.arraycopy(b, off, data, offset, len);
        offset += len;
    }

    /**
     * @return
     */
    public byte[] toByteArray() {
        return data.length == this.offset ? data : copy();
    }

    /**
     * @return
     */
    private byte[] copy() {
        byte[] data = new byte[this.offset];
        System.arraycopy(this.data, 0, data, 0, this.offset);
        return data;
    }

    /**
     * @param n
     */
    public void prependInt(int v) {
        int off = this.offset;
        this.offset = 0;
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
        this.offset = off;
    }
}
