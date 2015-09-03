package nidefawl.qubes.logging;

import java.io.*;

public class LogBufferStream extends PrintStream {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);


    public LogBufferStream(PrintStream out1) {
        super(out1);
    }

    @Override
    public synchronized void write(byte buf[], int off, int len) {
        try {
            super.write(buf, off, len);
            baos.write(buf, off, len);
        } catch (Exception e) {
        }
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public synchronized void flush() {
        super.flush();
        try {
            baos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void close() {
        super.close();
        try {
            baos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void write(int b) {
        super.write(b);
        baos.write(b);
    }

    public String getLogString() {
        try {
            return this.baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

}
