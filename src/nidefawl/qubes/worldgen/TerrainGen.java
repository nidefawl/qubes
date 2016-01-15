package nidefawl.qubes.worldgen;

import nidefawl.qubes.GameRegistry;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;
import nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain;

public class TerrainGen {

    public static void init() {
        GameRegistry.registerTerrainGenerator(TerrainGenBlockTest.GENERATOR_NAME, TerrainGenBlockTest.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorOther.GENERATOR_NAME, TerrainGeneratorOther.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorMain.GENERATOR_NAME, TerrainGeneratorMain.class);
        GameRegistry.registerTerrainGenerator(TerrainGenFlatSand128.GENERATOR_NAME, TerrainGenFlatSand128.class);
        GameRegistry.registerTerrainGenerator(TerrainGenQTest.GENERATOR_NAME, TerrainGenQTest.class);
        GameRegistry.registerChunkPopulator(ChunkPopulator.POPULATOR_NAME, ChunkPopulator.class);
        GameRegistry.registerChunkPopulator(EmptyChunkPopulator.POPULATOR_NAME, EmptyChunkPopulator.class);
    }

}
