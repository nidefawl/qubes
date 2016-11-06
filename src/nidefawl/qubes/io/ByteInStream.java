/**
 * 
 */
package nidefawl.qubes.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class ByteInStream extends InputStream {

    private final byte[] data;
    private int offset = 0;

    public ByteInStream(byte[] data) {
        this.data = data;
    }

    public int read() {
        return this.data[offset++] & 0xFF;
    }

    public int read(byte b[], int off, int len) throws IOException {
        System.arraycopy(this.data, this.offset, b, off, len);
        offset += len;
        return len;
    }

    public long skip(long n) {
        offset += n;
        return offset;
    }

    public int available() throws IOException {
        return this.data.length - offset;
    }

    public void close() {
        throw new UnsupportedOperationException();
    }

    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean markSupported() {
        return false;
    }
}
