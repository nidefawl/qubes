package nidefawl.qubes.test;

import java.io.*;
import java.util.Collection;
import java.util.Random;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.world.structure.StructureFactory;
import nidefawl.qubes.world.structure.StructureMap;
import nidefawl.qubes.world.structure.tree.Tree;

public class TestStructureMap {

    public static void main(String[] args) {
        try {
//            new TestStructureMap().test();
//            new TestStructureMap().test2();
            new TestStructureMap().test3();
            System.out.println("passed");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    
    public TestStructureMap() {
    }
    /**
     * @param trees1 
     * @throws IOException 
     * 
     */
    public void save(StructureMap<Tree> trees, File file) throws IOException {
        Compound cmp = new Compound();
        cmp.setInt("version", 1);
        Compound cmpMines = trees.save();
        cmp.set("trees", cmpMines);
        
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

    public void load(StructureMap<Tree> trees, File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            int len = dis.readInt();
            byte[] data = new byte[len];
            dis.readFully(data);
            dis.close();
            Compound cmp = (Compound) TagReader.readTagFromCompressedBytes(data);
            int version = cmp.getInt("version");
            Tag cmpMines = cmp.get("trees");
            if (cmpMines != null)
                trees.load((Tag.Compound)cmpMines);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    private void test2() {
        StructureMap<Tree> trees1 = newInstance();
        int offset = 16;
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
            {
                if (x==0&&z==0)continue;
                Tree m = new Tree();
                m.bb.set(x*offset-3, 0, z*offset-3, x*offset+3, 8, z*offset+3);
                m.trunkBB.set(m.bb);
                m.blocks = new int[0];
                trees1.add(m);
            }
        Collection<Tree> l1 = trees1.getRegions(0, 0, 13);
        for (Tree t : l1) {
            System.out.println(t.bb);
        }
    }
    private void test3() {
        StructureMap<Tree> trees1 = newInstance();
        Tree m = new Tree();
        m.bb.set(9605, 38764, -3975, 9611, 38770, -3969);
        m.trunkBB.set(9605, 38764, -3975, 9611, 38770, -3969);
        m.blocks = new int[0];
        trees1.add(m);
        trees1.validate();
    }
    private void test() {

        StructureMap<Tree> trees1 = newInstance();
        StructureMap<Tree> trees2 = newInstance();
        
        int ntrees = 100;
        int rounds = 11100;
        int maxCoord = 16000;
        Random rand = new Random(1);
        for (int a = 0; a < ntrees; a++) {
            int x = rand.nextInt(maxCoord*2)-maxCoord;
            int y = rand.nextInt(maxCoord*2)+maxCoord;
            int z = rand.nextInt(maxCoord*2)-maxCoord;
            int size = 3;
            Tree m = new Tree();
            m.bb.set(x-size, y, z-size, x+size, y+size*2, z+size);
            m.trunkBB.set(x-size, y, z-size, x+size, y+size*2, z+size);
            m.blocks = new int[0];
            trees1.add(m);
        }
        trees1.validate();
        File test = new File("test_structures.dat");
        try {
            save(trees1, test);
            load(trees2, test);
        } catch (IOException e) {
            e.printStackTrace();
        }
        trees2.validate();
//        this.trees.debug();
//        this.trees2.debug();
        for (int a = 0; a < rounds; a++) {
            int x = rand.nextInt(maxCoord*2)-maxCoord;
            int y = rand.nextInt(maxCoord*2)+maxCoord;
            int z = rand.nextInt(maxCoord*2)-maxCoord;
            Tree t1 = trees1.getNextStructure(x, y, z);
            Tree t2 = trees2.getNextStructure(x, y, z);
            boolean null1 = t1==null;
            boolean null2 = t2==null;
            if (null1 != null2) {
                throw new IllegalStateException();
            }
            if (null1) {
                continue;
            }
            if (!t1.bb.equalBB(t2.bb)) {
                throw new IllegalStateException();
            }
            Collection<Tree> l1 = trees1.getRegions(x, z, 16);
            Collection<Tree> l2 = trees2.getRegions(x, z, 16);
            if (l1.size() != l2.size()) {
                throw new IllegalStateException();
            }
            for (Tree t : l1) {
                if (!findTree(l2, t)) {
                    throw new IllegalStateException();
                }
            }
            
        }
    }
    static StructureMap<Tree> newInstance() {
        return new StructureMap<Tree>(new StructureFactory<Tree>() {
            @Override
            public Tree newInstance() {
                return new Tree();
            }
        });
    }


    static boolean findTree(Collection<Tree> l, Tree t2) {
        for (Tree t : l) {
            if (t.bb.equalBB(t2.bb)) {
                return true;
            }
        }
        return false;
    }
}
