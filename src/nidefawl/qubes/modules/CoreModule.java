/**
 * 
 */
package nidefawl.qubes.modules;

import nidefawl.qubes.GameRegistry;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class CoreModule extends Module {

    public CoreModule() {
    }

    @Override
    public void onLoad(Side side) {
        if (side == Side.SERVER) {
            GameRegistry.registerTerrainGenerator(TerrainGenBlockTest.GENERATOR_NAME, TerrainGenBlockTest.class);
            GameRegistry.registerTerrainGenerator(TerrainGeneratorOther.GENERATOR_NAME, TerrainGeneratorOther.class);
            GameRegistry.registerTerrainGenerator(TerrainGeneratorRivers.GENERATOR_NAME, TerrainGeneratorRivers.class);
            GameRegistry.registerTerrainGenerator(TerrainGenFlatSand128.GENERATOR_NAME, TerrainGenFlatSand128.class);
            GameRegistry.registerChunkPopulator(ChunkPopulator.POPULATOR_NAME, ChunkPopulator.class);
            GameRegistry.registerChunkPopulator(EmptyChunkPopulator.POPULATOR_NAME, EmptyChunkPopulator.class);
        }
    }

}
