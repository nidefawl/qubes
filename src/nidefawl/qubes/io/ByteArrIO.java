/**
 * 
 */
package nidefawl.qubes.io;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ByteArrIO {

    /**
     * @param output
     * @param n
     * @return
     */
    public static int writeInt(byte[] output, int offset, int v) {
        write(output, offset + 0, (v >>> 24) & 0xFF);
        write(output, offset + 1, (v >>> 16) & 0xFF);
        write(output, offset + 2, (v >>> 8) & 0xFF);
        write(output, offset + 3, (v >>> 0) & 0xFF);
        return 4;
    }
    public static int writeShort(byte[] output, int offset, int v) {
        write(output, offset + 0, (v >>> 8) & 0xFF);
        write(output, offset + 1, (v >>> 0) & 0xFF);
        return 2;
    }

    /**
     * Write a single byte
     * 
     * @param output
     * @param i
     */
    private static void write(byte[] output, int off, int v) {
        output[off] = (byte) v;
    }

    public static int readShort(byte[] out, int offset) {
        int ch3 = out[offset++] & 0xFF;
        int ch4 = out[offset++] & 0xFF;
        return (ch3 << 8) + (ch4);
    }

    public static int readUnsignedByte(byte[] out, int offset) {
        int ch4 = out[offset++] & 0xFF;
        return ch4;
    }

    /**
     * @param out
     * @param offset
     * @return
     */
    public static int readInt(byte[] out, int offset) {
        int ch1 = out[offset++] & 0xFF;
        int ch2 = out[offset++] & 0xFF;
        int ch3 = out[offset++] & 0xFF;
        int ch4 = out[offset++] & 0xFF;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    /**
     * @param output
     * @param offset
     * @param a
     */
    public static void writeByte(byte[] output, int offset, int v) {
        output[offset++] = (byte) (v & 0xFF);
    }
    public static void byteToShortArray(byte[] blocks, short[] dst) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = (short) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8) );
        }
    }
    public static byte[] shortToByteArray(short[] blocks) {
        byte[] bytes = new byte[blocks.length*2];
        return shortToByteArray(blocks, bytes);
    }
    public static byte[] shortToByteArray(short[] blocks, byte[] bytes) {
        for (int i = 0; i < blocks.length; i++) {
            bytes[i*2+0] = (byte) (blocks[i]&0xFF);
            bytes[i*2+1] = (byte) ((blocks[i]>>8)&0xFF);
        }
        return bytes;
    }
    public static short[] byteToShortArray(byte[] blocks) {
        short[] shorts = new short[blocks.length/2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8) );
        }
        return shorts;
    }

}
