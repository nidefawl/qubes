package nidefawl.qubes.nbt;

import java.io.*;
import java.util.zip.*;

import com.google.common.io.*;

public class TagReader {

    public static Tag readTagFromBytes(byte[] data) throws IOException {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        return Tag.read(input, TagReadLimiter.UNLIMITED);
    }

    public static Tag readTagFromBytesLimited(byte[] data) throws IOException {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        return Tag.read(input, new TagReadLimiter());
    }

    public static byte[] writeTagToBytes(Tag tag) throws IOException {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        Tag.write(tag, output);
        return output.toByteArray();
    }

    public static Tag readTagFromCompressedBytes(byte[] data) throws IOException {
        InflaterInputStream zipIn = new InflaterInputStream(new ByteArrayInputStream(data));
        return Tag.read(new DataInputStream(zipIn), TagReadLimiter.UNLIMITED);
    }

    public static byte[] writeTagToCompresedBytes(Tag tag) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        DeflaterOutputStream zipOut = new DeflaterOutputStream(ostream, new Deflater(3), 4 * 1024);
        DataOutputStream out = new DataOutputStream(zipOut);
        Tag.write(tag, out);
        zipOut.finish();
        return ostream.toByteArray();
    }

    public static Tag readTagFromFile(File f) throws IOException {
        ByteSource source = Files.asByteSource(f);
        byte[] data = source.read();
        return readTagFromCompressedBytes(data);
    }

    public static void writeTagToFile(Tag t, File f) throws IOException {
        byte[] data = writeTagToCompresedBytes(t);
        ByteSink sink = Files.asByteSink(f);
        sink.write(data);
    }
}
