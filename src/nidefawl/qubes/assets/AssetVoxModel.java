/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.*;

import nidefawl.qubes.models.loader.ModelVoxPalette;
import nidefawl.qubes.vec.BlockPos;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class AssetVoxModel extends Asset
{
    final static ModelVoxPalette defaultPalette = new ModelVoxPalette().fillDefault();
    final static String VOX_HEADER = "VOX ";
    final static String MAIN_CHUNK_HDR = "MAIN";
    final static String SIZE_CHUNK_HDR = "SIZE";
    final static String VOXEL_CHUNK_HDR = "XYZI";
    final static String PALETTE_CHUNK_HDR = "RGBA";
    public String name;
    public ModelVoxPalette palette;
    public BlockPos size;
    public int[] voxels;
    
    public AssetVoxModel() {
        this.name = "";
    }
    
    public AssetVoxModel(String name) {
        this.name = name;
    }
    
    public boolean headerCheck(byte[] data, String hdr) {
        for (int i = 0; i < hdr.length(); i++) {
            if (data[i] != hdr.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    public int readInt(DataInputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }
    /**
     * @param is
     * @throws IOException 
     */
    public void load(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        byte[] header = new byte[4];
        dis.readFully(header);
        if (!headerCheck(header, VOX_HEADER)) {
            throw new IOException("File is not a VOX file");
        }
        int version = readInt(dis);
        System.err.println("version "+version);
        while (dis.available() > 0) {
            dis.readFully(header);
            int chunkSize = readInt(dis);
            int chunkSizeChildren = readInt(dis);
            if (chunkSize < 0 || chunkSize > 1<<20) {
                throw new IOException("VOX model is too big: chunkSize "+chunkSize);
            }
            if (chunkSizeChildren < 0 || chunkSizeChildren > 1<<20) {
                throw new IOException("VOX model is too big: chunkSizeChildren "+chunkSizeChildren);
            }
            if (headerCheck(header, MAIN_CHUNK_HDR)) {
//                System.err.println("MAIN_CHUNK_HDR "+chunkSize);
            }
            else if (headerCheck(header, SIZE_CHUNK_HDR)) {
                int x = readInt(dis);
                int z = readInt(dis);
                int y = readInt(dis);
                this.size = new BlockPos(x, y, z);
            }
            else if (headerCheck(header, VOXEL_CHUNK_HDR)) {
                int numV = readInt(dis);
                this.voxels = new int[numV];
                for(int i = 0; i < numV; i++) {
                    int r = dis.readInt();
                    this.voxels[i] = r;
                }
            }
            else if (headerCheck(header, PALETTE_CHUNK_HDR)) {
                byte[] mainChunk = new byte[chunkSize];
                dis.readFully(mainChunk);
//                System.err.println("PALETTE_CHUNK_HDR "+chunkSize);
                this.palette = new ModelVoxPalette();
                this.palette.fromBytes(mainChunk);
            } else {

                System.err.println("CANT READ "+new String(header));
                byte[] mainChunk = new byte[chunkSize];
                dis.readFully(mainChunk);
            }
        }
        if (this.palette == null) {
            System.out.println("use default palette");
            this.palette = defaultPalette;
        } else {

            System.out.println("palette provided");
        }
        if (!complete())
            throw new IOException("Incomplete VOX model");
    }

    /**
     * @return 
     * @throws IOException 
     * 
     */
    private boolean complete() throws IOException {
        if (this.palette == null || this.voxels == null || this.size == null)
            return false;
        return true;
    }

}
