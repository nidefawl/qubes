/**
 * 
 */
package nidefawl.qubes.world.biomes;

import java.io.*;
import java.util.Collection;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.hex.HexCell;
import nidefawl.qubes.hex.HexagonGrid;
import nidefawl.qubes.hex.HexagonGridStorage;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.world.structure.StructureFactory;
import nidefawl.qubes.world.structure.StructureMap;
import nidefawl.qubes.world.structure.mine.Mine;
import nidefawl.qubes.world.structure.tree.Tree;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexBiome extends HexCell<HexBiome> {
    public boolean needsSave = false;
    public int version;
    public Biome biome;
    StructureMap<Tree> trees = new StructureMap<Tree>(new StructureFactory<Tree>() {
        @Override
        public Tree newInstance() {
            return new Tree();
        }
    });
    StructureMap<Mine> mines = new StructureMap<Mine>(new StructureFactory<Mine>() {
        @Override
        public Mine newInstance() {
            return new Mine();
        }
    });
    public int subtype;

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
            this.subtype = cmp.getInt("subtype");
            Tag cmpTree = cmp.get("trees");
            if (cmpTree != null)
                trees.load((Tag.Compound)cmpTree);
            Tag cmpMines = cmp.get("mines");
            if (cmpMines != null)
                mines.load((Tag.Compound)cmpMines);
            
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
        cmp.setInt("subtype", this.subtype);
        Compound cmpTrees = this.trees.save();
        Compound cmpMines = this.mines.save();
        cmp.set("trees", cmpTrees);
        cmp.set("mines", cmpMines);
        
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
        this.grid.flag(this.x, this.z);
    }

    public void registerMine(Mine m) {
        this.mines.add(m);
        this.grid.flag(this.x, this.z);
    }

    public Tree getTree(int x, int y, int z) {
        Collection<Tree> list = trees.getRegions(x, z, 0);
        for (Tree tree : list) {
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

    
    public StructureMap<Tree> getTrees() {
        return this.trees;
    }
    public StructureMap<Mine> getMines() {
        return this.mines;
    }

}
