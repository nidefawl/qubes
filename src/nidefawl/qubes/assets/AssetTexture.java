package nidefawl.qubes.assets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import nidefawl.qubes.texture.PNGDecoder;
import nidefawl.qubes.texture.TextureManager;

public class AssetTexture extends Asset {

    private File file;
    private int width;
    private int height;
    private byte[] data;
    private int glid;

    public AssetTexture(File file) {
        this.file = file;
    }

    public void load() throws Exception {
        FileInputStream fis = new FileInputStream(this.file);
        BufferedInputStream bif = new BufferedInputStream(fis);

        PNGDecoder dec = new PNGDecoder(bif);
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

    public void setupTexture() {
        this.glid = TextureManager.getInstance().makeNewTexture(this.data, this.width, this.height, true, false);
    }
    public int getGlid() {
        return glid;
    }
}
