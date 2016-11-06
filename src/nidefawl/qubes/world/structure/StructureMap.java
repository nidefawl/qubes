package nidefawl.qubes.world.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.plaf.synth.Region;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.RegionEntry;
import nidefawl.qubes.util.RegionMap;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.world.structure.tree.Tree;

public class StructureMap<T extends Structure> {
    private StructureFactory<T> fac;

    public StructureMap(StructureFactory<T> fac) {
        this.fac = fac;
    }
    protected RegionMap<T> structures = new RegionMap<T>(2);

    public void load(Tag.Compound nbttagcompound) {
        this.structures.clear();
        List list = nbttagcompound.getList("structures");
        for (int i = 0; i < list.size(); i++) {
            Tag.Compound cmpEntry = (Compound) list.get(i);
            Structure entry = this.fac.newInstance();
            entry.load(cmpEntry);
            this.structures.add((T)entry);
        }
    }

    public Tag.Compound save() {
        Tag.Compound cmp = new Tag.Compound();
        Tag.TagList list = new Tag.TagList();
        HashSet<Structure> removeDupes = new HashSet<Structure>();
        for (Structure s : this.structures.values()) {
            removeDupes.add(s);
        }
        for (Structure s : removeDupes) {
            list.add(s.save());
        }
        cmp.set("structures", list);
        return cmp;
    }
    public Collection<T> values() {
        return this.structures.values();
    }
    

    public boolean add(T s) {
//        if (!canBuildStructure(((RegionEntry)s).getBB())) {
//            return false;
//        }
        this.structures.add(s);
        return true;
    }

    public boolean containsAny(int x, int z, int i) {
        return structures.containsAny(x, z, i);
    }

    public int size() {
        return structures.values().size();
    }

    public boolean canBuildStructure(AABBInt outerBB) {
        Collection<T> structuresInBB = this.structures.getRegions(outerBB.minX, outerBB.minZ, outerBB.maxX, outerBB.maxZ);
        if (structuresInBB != null) {
            for (RegionEntry structure : structuresInBB) {
                if (structure.getBB().intersects(outerBB)) {
                    return false;
                }
            }
        }
        return true;
    }

    public T getNextStructure(int x, int y, int z) {
        return this.structures.getNearest(x, z);
    }

    public Collection<T> getRegions(int x, int z, int i) {
        return this.structures.getRegions(x, z, i);
    }

    public void debug() {
        this.structures.debug();
    }

    public void validate() {
        this.structures.validate();
    }

    public void clear() {
        this.structures.clear();
    }
}
