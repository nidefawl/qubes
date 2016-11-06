/**
 * 
 */
package nidefawl.qubes.world.biomes;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.network.packet.PacketSWorldBiomes;
import nidefawl.qubes.world.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class EmptyBiomeManager implements IBiomeManager {

    private BiomeSettingsStatic settings;
    public EmptyBiomeManager(World world, long seed, IWorldSettings settings, IBiomeSettings biomeSettings) {
        this.settings = (BiomeSettingsStatic) biomeSettings;
    }

    public EmptyBiomeManager(World world, long seed, IWorldSettings settings) {
        this.settings = new BiomeSettingsStatic();
    }

    @Override
    public Biome getBiome(int x, int z) {
        return Biome.MEADOW_GREEN;
    }

    @Override
    public int getBiomeFaceColor(World world, int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
        return Biome.MEADOW_GREEN.getFaceColor(colorType);
    }

    @Override
    public PacketSWorldBiomes getPacket() {
        return null;
    }
    @Override
    public void recvData(PacketSWorldBiomes packetSWorldBiomes) {
    }

    @Override
    public void sendChanges() {
    }

    @Override
    public void saveChanges() {
    }
    @Override
    public HexBiome blockToHex(int x, int z) {
        return null;
    }

    @Override
    public void deleteAll() {
    }

    @Override
    public IBiomeSettings getBiomeSettings() {
        return this.settings;
    }
}
