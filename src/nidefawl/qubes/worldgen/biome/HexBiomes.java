/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexagonGrid;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiomes implements IBiomeManager {
    public final HexagonGrid grid = new HexagonGrid(128);
    final static public Pattern FILE_PATTERN = Pattern.compile("hex\\.(-?[0-9]+)\\.(-?[0-9]+)\\.dat");
	private final File dir;
	HashMap<Long, HexBiome> map = new HashMap<>(); //TODO: replace with fixed size array

	public HexBiomes(WorldServer world, long seed, WorldSettings settings) {
	    this(new File(((WorldSettings) world.settings).getWorldDirectory(), "biomes"));
	}
	public HexBiomes(File dir) {
        this.dir = dir;
        this.dir.mkdirs();
        System.out.println("HexBiomes at "+dir.getAbsolutePath());
        File[] regionFiles = dir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".dat");
            }
        });
        int n = 0;
        for (int i = 0; regionFiles != null && i < regionFiles.length; i++) {
            File f = regionFiles[i];
            Matcher m = FILE_PATTERN.matcher(f.getName());
            if (m.matches()) {
                int x = StringUtil.parseInt(m.group(1), 0);
                int z = StringUtil.parseInt(m.group(1), 0);
                HexBiome biome = new HexBiome(x, z, f);
                try {
                    biome.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}

    //TODO: make threadsafe
    public Biome getBiome(int x, int z) {
        long pos = this.grid.blockToGrid(x, z);
        HexBiome b = this.map.get(pos); 
        if (b == null) {
            int gridX = GameMath.lhToX(pos);
            int gridY = GameMath.lhToZ(pos);
            b = new HexBiome(gridX, gridY, new File(this.dir, String.format("hex.%d.%d.dat", gridX, gridY)));
            this.map.put(pos, b);
            try {
                if (b.file.exists()) {
                    b.load();    
                } else {
                    //This is really only for testing
                    int id = new Random(gridX*89153^gridY*31+1).nextInt(2);
                    b.biome = Biome.biomes[id];
                    b.save();
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return b.biome;
    }
}
