/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import java.io.*;
import java.util.*;

import com.google.common.collect.Sets;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexCell;
import nidefawl.qubes.hex.HexagonGrid;
import nidefawl.qubes.hex.HexagonGridStorage;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.RegionMap;
import nidefawl.qubes.worldgen.trees.Tree;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiome extends HexCell<HexBiome> {
    public boolean needsSave = false;
    public int version;
    public Biome biome;
    Set<Tree> treeList = Sets.newConcurrentHashSet();
    RegionMap<Tree> trees = new RegionMap<Tree>(2);

    public HexBiome(HexagonGridStorage<HexBiome> hexBiomes, int x, int z) {
        super(hexBiomes, x, z);
    }

    /**
     * @param file 
     * @throws IOException 
     * 
     */
    public void load(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            int len = dis.readInt();
            byte[] data = new byte[len];
            dis.readFully(data);
            dis.close();
            Compound cmp = (Compound) TagReader.readTagFromCompressedBytes(data);
            this.version = cmp.getInt("version");
            this.x = cmp.getInt("x");
            this.z = cmp.getInt("z");
            this.biome = Biome.get(cmp.getInt("biome"));
            List list = cmp.getList("trees");
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Compound cmpTree = (Compound) it.next();
                Tree tree = new Tree();
                tree.load(cmpTree);
                treeList.add(tree);
                trees.add(tree);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    /**
     * @throws IOException 
     * 
     */
    public void save(File file) throws IOException {
        Compound cmp = new Compound();
        cmp.setInt("version", 1);
        cmp.setInt("x", x);
        cmp.setInt("z", z);
        cmp.setInt("biome", this.biome.id);
        Iterator<Tree> treeIt = this.treeList.iterator();
        Tag.TagList list = new Tag.TagList();
        while (treeIt.hasNext()) {
            Tree tree = treeIt.next();
            Compound compound = tree.save();
            list.add(compound);
        }
        cmp.set("trees", list);
        
        //TODO: move to thread!
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        try {
            fos = new FileOutputStream(file);
            dos = new DataOutputStream(new BufferedOutputStream(fos));
            byte[] data = TagReader.writeTagToCompresedBytes(cmp);
            dos.writeInt(data.length);
            dos.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                dos.flush();
            }
            if (fos != null) {
                fos.close(); 
            }
        }
    }

    public void registerTree(Tree tree) {
        this.trees.add(tree);
        this.treeList.add(tree);
        this.grid.flag(this.x, this.z);
    }

    public Tree getTree(int x, int y, int z) {
        Collection<Tree> list = trees.getRegions(x, z, 0);
        for (Tree tree : list) {
            System.err.println(tree);
            System.err.println(""+x+","+y+","+z+" - "+tree.trunkBB);
            if (!tree.trunkBB.contains(x, y, z)) {
                continue;
            }
            if (!tree.has(x, y, z)) {
                continue;
            }
            return tree;
        }
        return null;
    }

    public HexBiome[] getClosest3(double blockx, double blockz) {
        int n = getClosesCorner(blockx, blockz);
        int offset = 5-n;
        long k1 = HexagonGrid.offset(this.x, this.z, (offset+0)%6);
        HexBiome hex1 = (HexBiome) grid.getPos(k1);
        long k2 = HexagonGrid.offset(this.x, this.z, (offset+1)%6);
        HexBiome hex2 = (HexBiome) grid.getPos(k2);
        return new HexBiome[] { this, hex1, hex2 };
    }

    public Collection<Tree> getNearbyTrees(int x, int y, int z, int i) {
        Collection<Tree> list = trees.getRegions(x, z, i);
        return list;
    }

}
