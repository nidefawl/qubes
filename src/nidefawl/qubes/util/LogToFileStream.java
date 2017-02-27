package nidefawl.qubes.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LogToFileStream extends PrintStream {
    private final PrintStream out;

    public LogToFileStream(PrintStream out1, FileOutputStream fos) {
        super(out1);
        this.out = new PrintStream(new BufferedOutputStream(fos));
    }

    @Override
    public void write(byte buf[], int off, int len) {
        try {
            super.write(buf, off, len);
            out.write(buf, off, len);
        } catch (Exception e) {
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void flush() {
        super.flush();
        out.flush();
    }

    @Override
    public void close() {
        super.close();
        out.flush();
    }

    @Override
    public void write(int b) {
        super.write(b);
        out.write(b);
    }
}