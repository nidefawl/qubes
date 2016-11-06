package nidefawl.qubes.world.structure;

import java.util.*;

import com.google.common.collect.Sets;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockLog;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.World;

public class Structure implements RegionEntry {

    Set<Integer> keys;
    public final AABBInt bb = new AABBInt();
    public int[] blocks;
    public Structure() {
    }
    @Override
    public Set<Integer> getKeys() {
        return keys == null ? Collections.<Integer>emptySet() : keys;
    }

    @Override
    public void addKey(int key) {
        if (keys==null)keys=Sets.newConcurrentHashSet();
        keys.add(key);
    }

    @Override
    public void removeKey(int key) {
        if (keys==null)return;
        keys.remove(key);
    }

    @Override
    public int getMinX() {
        return bb.minX;
    }

    @Override
    public int getMinZ() {
        return bb.minZ;
    }

    @Override
    public int getMaxX() {
        return bb.maxX;
    }

    @Override
    public int getMaxZ() {
        return bb.maxZ;
    }
    @Override
    public AABBInt getBB() {
        return bb;
    }


    public Compound save() {
        Tag.Compound tag = new Tag.Compound();
        Tag.Compound treeBB = this.bb.saveTag();
        tag.set("bb", treeBB);
        byte[] blocksBytes = new byte[this.blocks.length<<2];
        intToByteArray(this.blocks, blocksBytes);
        tag.setByteArray("blocks", blocksBytes);
        return tag;
    }

    public void load(Compound cmpTree) {
        Tag.Compound treeBB = (Compound) cmpTree.get("bb");
        this.bb.loadTag(treeBB);
        byte[] blocksBytes = cmpTree.getByteArray("blocks").getArray();
        int[] blocksInts = new int[blocksBytes.length>>2];
        byteToIntArray(blocksBytes, blocksInts);
        Arrays.sort(blocksInts);
        this.blocks = blocksInts;
    }

    public static byte[] intToByteArray(int[] blocks, byte[] bytes) {
        for (int i = 0; i < blocks.length; i++) {
            int blockid = blocks[i];
            for (int a = 0; a < 4; a++) {
                bytes[i*4+a] = (byte) (blockid&0xFF);
                blockid >>= 8;
            }
        }
        return bytes;
    }

    public static int[] byteToIntArray(byte[] blocks, int[] ints) {
        for (int i = 0; i < ints.length; i++) {
            int j = 0;
            for (int a = 0; a < 4; a++) {
                int d = (blocks[i*4+a]&0xFF) << (a*8);
                j |= d;
            }
            ints[i] = j;
        }
        return ints;
    }

    public boolean contains(final int x, final int y, final int z) {
        return this.bb.contains(x, y, z);
    }

    public Iterator<BlockPos> iterator() {
        return new StructureBlockIterator(this, this.bb);
    }
    public void setBlocks(long[] blocks) {
        this.blocks = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            long lPos = blocks[i];
            int x1 = TripletLongHash.getX(lPos)-this.bb.minX;
            int y1 = TripletLongHash.getY(lPos)-this.bb.minY;
            int z1 = TripletLongHash.getZ(lPos)-this.bb.minZ;
            this.blocks[i] = TripletIntHash.toHash(x1, y1, z1);
        }
        Arrays.sort(this.blocks);
    }

    public boolean has(final int x, final int y, final int z) {
        int hash = TripletIntHash.toHash(x-this.bb.minX, y-this.bb.minY, z-this.bb.minZ);
        int idx = Arrays.binarySearch(this.blocks, hash);
        return idx >= 0;
    }
}
