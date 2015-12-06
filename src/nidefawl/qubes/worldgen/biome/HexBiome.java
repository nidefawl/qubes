/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import java.io.*;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.network.packet.Packet;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiome {

    public final File file;
    public int version;
    public int x;
    public int z;
    public Biome biome;

    public HexBiome(int x, int z, File file) {
        this.x = x;
        this.z = z;
        this.file = file;
    }

    /**
     * @throws IOException 
     * 
     */
    public void load() throws IOException {
        if (this.file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(this.file);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
                this.version = dis.readInt();
                int id = dis.readInt();
                this.biome = Biome.get(id);
                dis.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }
    /**
     * @throws IOException 
     * 
     */
    public void save() throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.file);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
            dos.writeInt(this.version);
            dos.writeInt(this.biome.id);
            dos.flush();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos.close(); 
            }
        }
    }

}
