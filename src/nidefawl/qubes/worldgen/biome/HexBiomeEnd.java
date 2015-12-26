package nidefawl.qubes.worldgen.biome;

import java.io.File;
import java.io.IOException;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexagonGridStorage;
import nidefawl.qubes.worldgen.trees.Tree;

public class HexBiomeEnd extends HexBiome {
    public HexBiomeEnd(HexagonGridStorage<HexBiome> grid, int x, int z) {
        super(grid, x, z);
        this.biome = Biome.MEADOW_GREEN;
    }
    @Override
    public void save(File file) throws IOException {
    }
    @Override
    public void registerTree(Tree tree) {
    }
    @Override
    public void load(File file) throws IOException {
    }
    
}