package nidefawl.qubes.util;

import java.io.EOFException;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.vec.Quaternion;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class BinaryStreamReader {
    public int offset = 0;
    public AssetBinary asset;

    byte[] readBytes(int len) throws EOFException {
        byte[] data = this.asset.getData();
        if (offset+len-1>=data.length)
            throw new EOFException();
        byte[] rawdata = new byte[len];
        System.arraycopy(data, this.offset, rawdata, 0, len);
        this.offset += len;
        return rawdata;
    }

    int[] readUByteArray(int i) throws EOFException {
        byte[] data = readBytes(i);
        int[] unsigned = new int[i];
        for (int j = 0; j < i; j++) {
            unsigned[j] = data[j] & 0xFF;
        }
        return unsigned;
    }
    public float readFloat() throws EOFException {
        int floatBits = readInt();
        return Float.intBitsToFloat(floatBits);
    }
    public double readDouble() throws EOFException {
        long lsb;
        long msb;
        lsb = readInt()&0xFFFFFFFFL;
        msb = readInt()&0xFFFFFFFFL;
        return Double.longBitsToDouble(msb<<32|lsb);
    }
    
    public Vector3f readVec3() throws EOFException {
        return new Vector3f(readFloat(), readFloat(), readFloat());
    }
    
    public Quaternion readQuaternion() throws EOFException {
        return new Quaternion(readFloat(), readFloat(), readFloat(), readFloat());
    }
    
    public Vector4f readVec4() throws EOFException {
        return new Vector4f(readFloat(), readFloat(), readFloat(), readFloat());
    }
    
    public int readInt() throws EOFException {
        byte[] data = this.asset.getData();
        if (offset+3>=data.length)
            throw new EOFException();
        int ch1 = data[offset++]&0xFF;
        int ch2 = data[offset++]&0xFF;
        int ch3 = data[offset++]&0xFF;
        int ch4 = data[offset++]&0xFF;
        return ((ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }
    public int readUShort() throws EOFException {
        byte[] data = this.asset.getData();
        if (offset+1>=data.length)
            throw new EOFException();
        int ch1 = data[offset++]&0xFF;
        int ch2 = data[offset++]&0xFF;
        return (ch1 << 0) + (ch2 << 8);
    }
    public int readUByte() throws EOFException {
        byte[] data = this.asset.getData();
        if (offset>=data.length)
            throw new EOFException();
        return data[offset++]&0xFF;
    }
    public int readSByte() throws EOFException {
        byte[] data = this.asset.getData();
        if (offset>=data.length)
            throw new EOFException();
        return data[offset++];
    }
    public String readString(int len) throws EOFException {
        byte[] strBytes = readBytes(len);
        int strlen = 0;
        for (int i = 0; i < strBytes.length && strBytes[i] != 0; i++, strlen++);
        return new String(strBytes, 0, strlen);
    }

    public void resetOffset() {
        this.offset = 0;
    }

}
