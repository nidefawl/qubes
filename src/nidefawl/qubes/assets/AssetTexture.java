package nidefawl.qubes.assets;

import java.io.*;
import java.nio.ByteBuffer;

import nidefawl.qubes.texture.PNGDecoder;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class AssetTexture extends Asset {

    private int width;
    private int height;
    private byte[] data;
    int slot = -1;
    
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

    /**
     * @param tileSize
     */
    public void rescale(int tileSize) {
        double scale = tileSize/(double)this.width;
        int iscale = (int) scale;
        if (scale != iscale) {
            throw new GameError("Cannot scale texture by non-integer scale factor");
        }
        byte[] newData = new byte[tileSize*tileSize*4];
        for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.height; z++) {
                int idx1 = ((x)+(z)*this.width)*4;
                for (int x2 = 0; x2 < iscale; x2++) {
                    for (int z2 = 0; z2 < iscale; z2++) {
                        int idx2 = ((x*iscale+x2)+(z*iscale+z2)*tileSize)*4;
                        for (int i = 0; i < 4; i++) {
                            newData[idx2+i] = this.data[idx1+i];
                        }
                    }   
                }
            }
        }
        this.data = newData;
        this.width = tileSize;
        this.height = tileSize;
    }

    /**
     * @param width2
     */
    public void cutH() {
        byte[] newData = new byte[this.width*this.width*4];
        for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.width; z++) {
                int idx1 = ((x)+(z)*this.width)*4;
                for (int i = 0; i < 4; i++) {
                    newData[idx1+i] = this.data[idx1+i];
                }
            }
        }
        this.data = newData;
        this.height = this.width;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
}
