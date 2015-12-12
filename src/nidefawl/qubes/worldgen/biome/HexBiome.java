/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import java.io.*;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexCell;
import nidefawl.qubes.hex.HexagonGridStorage;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.world.WorldServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiome extends HexCell<HexBiome> {

    public int version;
    public Biome biome;

    public HexBiome(HexagonGridStorage<HexBiome> hexBiomes, int x, int z) {
        super(hexBiomes, x, z);
    }

    /**
     * @param file 
     * @throws IOException 
     * 
     */
    public void load(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
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
    /**
     * @throws IOException 
     * 
     */
    public void save(File file) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
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
