/**
 * 
 */
package nidefawl.qubes.biomes;

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
import nidefawl.qubes.util.*;
import nidefawl.qubes.world.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
@SideOnly(value = Side.SERVER)
public class HexBiomesServer extends HexBiomes {
    final static public Pattern FILE_PATTERN = Pattern.compile("hex\\.(-?[0-9]+)\\.(-?[0-9]+)\\.dat");
    public static boolean SAVE_LOAD = true;
	private final File dir;
    Set<Long>       flaggedInstances = Sets.newConcurrentHashSet();
    Set<Long>       flaggedInstances2 = Sets.newConcurrentHashSet();

	public HexBiomesServer(World world, long seed, IWorldSettings settings) {
	    super(world, seed, settings);
        this.dir = new File(((WorldSettings) settings).getWorldDirectory(), "biomes");
        this.dir.mkdirs();
        if (SAVE_LOAD) {
            loadFiles();
        }
	}
	File getFile(int x, int z) {

        File file = new File(this.dir, String.format("hex.%d.%d.dat", x, z));
        return file;
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
       SAVE_LOAD = false;
    }

    //TODO: make threadsafe
    @Override
    public HexBiome loadCell(int gridX, int gridY) {
        HexBiome b = new HexBiome(this, gridX, gridY);
        //This is really only for testing
        Random rand = new Random(gridX * 89153 ^ gridY * 33199 + 1);
        
        int id = rand.nextInt(Biome.maxBiome);
        int subtype = rand.nextInt(16);
        System.out.println("biome at "+gridX+","+gridY+": "+id);
        b.biome = Biome.biomes[id];
        b.subtype = subtype;
        if (SAVE_LOAD) {
            try {
                b.save(getFile(gridX, gridY));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("save cell "+gridX+"/"+gridY);   
        }
        this.flagBiome(gridX, gridY);
        return b;
    }

    private synchronized void flagBiome(int gridX, int gridY) {
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
    public synchronized void sendChanges() {
        if (!this.flaggedInstances.isEmpty()) {
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
    @Override
    public synchronized void saveChanges() {
        if (!this.flaggedInstances2.isEmpty()) {
            for (Long e : this.flaggedInstances2) {
                HexBiome hexBiome = getPos(e);
                try {
//                    long l = System.nanoTime();
                    hexBiome.save(getFile(hexBiome.x, hexBiome.z));
//                    long l2 = System.nanoTime();
//                    System.out.println("save took "+(l2-l)/1000000L);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            flaggedInstances2.clear();
        }
    }
    @Override
    public synchronized void flag(int x, int z) {
        flaggedInstances2.add(GameMath.toLong(x, z));
    }

    @Override
    public void deleteAll() {
        synchronized (this) {
            File[] regionFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".dat");
                }
            });
            for (int i = 0; regionFiles != null && i < regionFiles.length; i++) {
                File f = regionFiles[i];
                Matcher m = FILE_PATTERN.matcher(f.getName());
                if (m.matches()) {
                    f.delete();
                }
            }
            this.flaggedInstances.clear();
            this.flaggedInstances2.clear();
            super.reset();
        }
    }
}
