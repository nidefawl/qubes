/**
 * 
 */
package nidefawl.qubes.world.biomes;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.network.packet.PacketSWorldBiomes;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiomesClient extends HexBiomes {

	public HexBiomesClient(World world, long seed, IWorldSettings settings) {
	    super(world, seed, settings);
	}
    @Override
    public HexBiome loadCell(int gridX, int gridY) {
        return null;
    }
    @Override
    public HexBiome oobCell(int x, int z) {
        return new HexBiomeEnd(this, x, z);
    }

    @Override
    public PacketSWorldBiomes getPacket() {
        return null;
    }
    @Override
    public void recvData(PacketSWorldBiomes p) {
        for (int i = 0; i < p.numBiomes; i++) {
            int x = p.coordsX[i];
            int z = p.coordsZ[i];
            HexBiome cell = new HexBiome(this, x, z);
            cell.biome = Biome.get(p.biomes[i]&0xFF);
            putPos(GameMath.toLong(x, z), cell);
        }
    }

    @Override
    public void sendChanges() {
    }
    @Override
    public void saveChanges() {
    }

    @Override
    public void deleteAll() {
    }
}
