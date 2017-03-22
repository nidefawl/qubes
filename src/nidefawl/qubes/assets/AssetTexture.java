package nidefawl.qubes.assets;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import nidefawl.qubes.texture.DDSLoader;
import nidefawl.qubes.texture.DDSLoader.Format;
import nidefawl.qubes.texture.PNGDecoder;
import nidefawl.qubes.util.GameError;

public class AssetTexture extends Asset {
    public static enum Type {
        PNG, DDS
    };
    private int width;
    private int height;
    private byte[] data;
    int slot = -1;
    private String name;
    private int bits;
    private int colorComps;
    private short[] shortData;
    private DDSLoader dds;
    
    public AssetTexture() {
        this.name = "";
    }
    
    public AssetTexture(String name) {
        this.name = name;
    }
    private boolean loadImageIO(AssetInputStream is) throws Exception {
        setPack(is.source);
        BufferedImage bufferedImage = ImageIO.read(is.inputStream);
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
        DataBuffer buf = bufferedImage.getRaster().getDataBuffer();
        
        this.bits = DataBuffer.getDataTypeSize(buf.getDataType());
        if (bits != 8 && bits != 16) {
            throw new IOException("Unsupported bit depth: "+bits);
        }
        
        if (bits == 16) {
            if (bufferedImage.getType() != BufferedImage.TYPE_USHORT_GRAY) {
                throw new IOException("Unsupported 16bit image format");
            }
            if (!(buf instanceof DataBufferUShort)) {
                throw new IOException("Unsupported 16bit image format");
            }
            DataBufferUShort b = ((java.awt.image.DataBufferUShort) bufferedImage.getRaster().getDataBuffer());
            short[] shData = b.getData();
            this.shortData = new short[shData.length];
            System.arraycopy(shData, 0, this.shortData, 0, shData.length);
            this.colorComps = 1;
        } else if (bits == 8) {
            //TODO: implement
            throw new IOException("Unsupported 8bit image format");
        }
        
        return true;
    }


    private boolean loadDDS(AssetInputStream is) throws Exception {
        setPack(is.source);
        DDSLoader dec = null;
        try {
            dec = new DDSLoader(is.inputStream);
            ByteBuffer data = dec.load(true);
            this.width = dec.getWidth();
            this.height = dec.getHeight();
            this.bits = dec.getBitdepth();
            if (this.bits != 8) { //16 bit broken
                return false;
            }
            Format fmt = dec.getPixelFormat();
            
            this.colorComps = fmt.getComponents();
            this.data = data.array();
        } finally {
            this.dds = dec;
        }
        return true;
    }

    private boolean loadPNGDecoder(AssetInputStream is) throws Exception {
        setPack(is.source);
        PNGDecoder dec = new PNGDecoder(is.inputStream);
        this.width = dec.getWidth();
        this.height = dec.getHeight();
        this.bits = dec.getBitdepth();
        if (this.bits != 8) { //16 bit broken
            return false;
        }
        PNGDecoder.Format fmt = PNGDecoder.Format.RGBA;
        fmt = dec.decideTextureFormat(fmt);
        if (fmt != PNGDecoder.Format.RGBA && fmt != PNGDecoder.Format.LUMINANCE) {
            return false;
        }
        
        this.colorComps = fmt.getNumComponents();
        int bufferSize = width*height*(fmt.getNumComponents()*(this.bits/8));
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize); 
        dec.decode(buffer, width*fmt.getNumComponents(), fmt);
        this.data = buffer.array();
        return true;
    }
    public int getBits() {
        return this.bits;
    }
    public int getComponents() {
        return this.colorComps;
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

    /**
     * @return the texture path
     */
    public String getName() {
        return this.name;
    }

    public short[] getUShortData() {
        return this.shortData;
    }

    public void load(Type type, AssetInputStream is) throws IOException, Exception {
        if (type == Type.DDS) {
            loadDDS(is);
        }
        if (type == Type.PNG) {
            if (!loadPNGDecoder(is)) {
                is = is.source.getInputStream(name);
                loadImageIO(is);
            }
        }
    }
    public DDSLoader getDds() {
        return this.dds;
    }
}
