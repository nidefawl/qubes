package nidefawl.qubes.assets;

import java.io.*;
import java.nio.ByteBuffer;

import nidefawl.qubes.texture.PNGDecoder;

public class AssetTexture extends Asset {

    private int width;
    private int height;
    private byte[] data;

    public AssetTexture() {
    }

    public void load(InputStream is) throws Exception {

        PNGDecoder dec = new PNGDecoder(is);
        this.width = dec.getWidth();
        this.height = dec.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width*height*4); 
        dec.decode(buffer, width*4, PNGDecoder.Format.RGBA);
        this.data = buffer.array();
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public byte[] getData() {
        return data;
    }
}
