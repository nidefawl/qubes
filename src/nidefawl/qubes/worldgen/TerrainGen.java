package nidefawl.qubes.worldgen;

import nidefawl.qubes.GameRegistry;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;
import nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorLight;
import nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain;
import nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorTest2;

@SideOnly(value = Side.SERVER)
public class TerrainGen {

    public static void init() {
        GameRegistry.registerTerrainGenerator(TerrainGenBlockTest.GENERATOR_NAME, TerrainGenBlockTest.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorOther.GENERATOR_NAME, TerrainGeneratorOther.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorMain.GENERATOR_NAME, TerrainGeneratorMain.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorTest2.GENERATOR_NAME, TerrainGeneratorTest2.class);
        GameRegistry.registerTerrainGenerator(TerrainGenFlatSand128.GENERATOR_NAME, TerrainGenFlatSand128.class);
        GameRegistry.registerTerrainGenerator(TerrainGenMines.GENERATOR_NAME, TerrainGenMines.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorLight.GENERATOR_NAME, TerrainGeneratorLight.class);
        GameRegistry.registerTerrainGenerator(TerrainGenQTest.GENERATOR_NAME, TerrainGenQTest.class);
        GameRegistry.registerTerrainGenerator(TerrainGeneratorIsland.GENERATOR_NAME, TerrainGeneratorIsland.class);
        GameRegistry.registerChunkPopulator(ChunkPopulator.POPULATOR_NAME, ChunkPopulator.class);
        GameRegistry.registerChunkPopulator(EmptyChunkPopulator.POPULATOR_NAME, EmptyChunkPopulator.class);
    }

}
