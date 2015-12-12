/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexCell;
import nidefawl.qubes.network.packet.PacketSWorldBiomes;
import nidefawl.qubes.server.PlayerChunkTracker.Entry;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.world.*;
import nidefawl.qubes.worldgen.biome.HexBiomes.HexBiomeEnd;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiomesServer extends HexBiomes {
    final static public Pattern FILE_PATTERN = Pattern.compile("hex\\.(-?[0-9]+)\\.(-?[0-9]+)\\.dat");
	private final File dir;
    Set<Long>       flaggedInstances = Sets.newConcurrentHashSet();

	public HexBiomesServer(World world, long seed, IWorldSettings settings) {
	    super(world, seed, settings);
        this.dir = new File(((WorldSettings) settings).getWorldDirectory(), "biomes");
        this.dir.mkdirs();
        loadFiles();
	}
    
	/**
     * 
     */
    private void loadFiles() {
        System.out.println("HexBiomes at "+dir.getAbsolutePath());
        File[] regionFiles = dir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".dat");
            }
        });
        System.out.println("loading "+regionFiles.length+" biome files");
        int n = 0;
        for (int i = 0; regionFiles != null && i < regionFiles.length; i++) {
            File f = regionFiles[i];
            Matcher m = FILE_PATTERN.matcher(f.getName());
            if (m.matches()) {
                int x = StringUtil.parseInt(m.group(1), 0);
                int z = StringUtil.parseInt(m.group(2), 0);
                HexBiome biome = new HexBiome(this, x, z);
                try {
                    biome.load(f);
                    putPos(GameMath.toLong(x, z), biome);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //DEBUG CONSTRUCTOR FOR USE OUTSIDE OF GAME 
    public HexBiomesServer(File file) {
        super(null, 0L, null);
        this.dir = file;
        this.dir.mkdirs();
        loadFiles();
    }

    //TODO: make threadsafe
    @Override
    public HexBiome loadCell(int gridX, int gridY) {
        File file = new File(this.dir, String.format("hex.%d.%d.dat", gridX, gridY));
        HexBiome b = new HexBiome(this, gridX, gridY);
        //This is really only for testing
        int id = new Random(gridX * 89153 ^ gridY * 33199 + 1).nextInt(Biome.maxBiome);
        System.out.println("biome at "+gridX+","+gridY+": "+id);
        b.biome = Biome.biomes[id];
        try {
            b.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("save cell "+gridX+"/"+gridY);
        this.flagBiome(gridX, gridY);
        return b;
    }

    private void flagBiome(int gridX, int gridY) {
        flaggedInstances.add(GameMath.toLong(gridX, gridY));
    }

    //TODO: compress or make ensure we never have too much data to send
    @Override
    public PacketSWorldBiomes getPacket() {
        ArrayList<HexBiome> biomes = new ArrayList<>();
        for (HexCell c : this.getLoaded()) {
            biomes.add((HexBiome) c);
        }
        PacketSWorldBiomes biomesPacket = makePacket(biomes);
        return biomesPacket;
    }
    /**
     * @param biomes
     * @return
     */
    private PacketSWorldBiomes makePacket(ArrayList<HexBiome> biomes) {
        int len = biomes.size();
        PacketSWorldBiomes biomesPacket = new PacketSWorldBiomes();
        biomesPacket.numBiomes = len;
        //maybe use relative coordinates and pack them into bytes (so we can write(byteArray)) 
        biomesPacket.coordsX = new int[len];
        biomesPacket.coordsZ = new int[len];
        biomesPacket.biomes = new byte[len];
        for (int i = 0; i < len; i++) {
            HexBiome b = biomes.get(i);
            biomesPacket.coordsX[i] = b.x;
            biomesPacket.coordsZ[i] = b.z;
            biomesPacket.biomes[i] = (byte) b.biome.id;
        }
        return biomesPacket;
    }

    @Override
    public void recvData(PacketSWorldBiomes packetSWorldBiomes) {
    }

    /**
     * Send block changes.
     */
    public void sendChanges() {
        if (!this.flaggedInstances.isEmpty()) {
            //TODO: drain atomic! (.iterator().remove() ?)
            ArrayList<HexBiome> biomes = new ArrayList<>();
            for (Long e : this.flaggedInstances) {
                HexBiome hexBiome = getPos(e);
                biomes.add(hexBiome);
            }
            flaggedInstances.clear();
            PacketSWorldBiomes biomesPacket = makePacket(biomes);
            ((WorldServer)this.world).broadcastPacket(biomesPacket);
        }
    }
    @Override
    public HexBiome oobCell(int x, int z) {
        return new HexBiomeEnd(this, x, z);
    }
}
