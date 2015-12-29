/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.network.packet.PacketSWorldBiomes;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IBiomeManager {

    
    public Biome getBiome(int x, int z);

    /**
     * @return
     */
    public int getWorldType();

    /**
     * @param world
     * @param x
     * @param y
     * @param z
     * @param faceDir
     * @param pass
     * @param colorType
     * @return
     */
    public int getBiomeFaceColor(World world, int x, int y, int z, int faceDir, int pass, BiomeColor colorType);

    /** May return null
     * @param world 
     * @return
     */
    public PacketSWorldBiomes getPacket();

    /**
     * @param packetSWorldBiomes
     */
    public void recvData(PacketSWorldBiomes packetSWorldBiomes);

    /**
     * 
     */
    public void sendChanges();

    public void saveChanges();

    public HexBiome blockToHex(int x, int z);

    public void deleteAll();
}
