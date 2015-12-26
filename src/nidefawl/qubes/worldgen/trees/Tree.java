package nidefawl.qubes.worldgen.trees;

import java.util.*;

import com.google.common.collect.Sets;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.World;

public class Tree implements RegionEntry {

    Set<Integer> keys;
    public final AABBInt bb = new AABBInt();
    public final AABBInt trunkBB = new AABBInt();
    int[] blocks;
    public Tree() {
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

    public Compound save() {
        Tag.Compound tag = new Tag.Compound();
        Tag.Compound treeBB = this.bb.saveTag();
        Tag.Compound trunkBB = this.trunkBB.saveTag();
        tag.set("treeBB", treeBB);
        tag.set("trunkBB", trunkBB);
        byte[] blocksBytes = new byte[this.blocks.length<<2];
        intToByteArray(this.blocks, blocksBytes);
        tag.setByteArray("blocks", blocksBytes);
        return tag;
    }

    public void load(Compound cmpTree) {
        Tag.Compound treeBB = (Compound) cmpTree.get("treeBB");
        Tag.Compound trunkBB = (Compound) cmpTree.get("trunkBB");
        this.bb.loadTag(treeBB);
        this.trunkBB.loadTag(trunkBB);
        byte[] blocksBytes = cmpTree.getByteArray("blocks").getArray();
        int[] blocksInts = new int[blocksBytes.length>>2];
        byteToIntArray(blocksBytes, blocksInts);
        Arrays.sort(blocksInts);
        this.blocks = blocksInts;
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
    

    public static byte[] intToByteArray(int[] blocks, byte[] bytes) {
        for (int i = 0; i < blocks.length; i++) {
            bytes[i*2+0] = (byte) (blocks[i]&0xFF);
            bytes[i*2+1] = (byte) ((blocks[i]>>8)&0xFF);
            bytes[i*2+2] = (byte) ((blocks[i]>>16)&0xFF);
            bytes[i*2+3] = (byte) ((blocks[i]>>24)&0xFF);
        }
        return bytes;
    }

    public static int[] byteToIntArray(byte[] blocks, int[] ints) {
        for (int i = 0; i < ints.length; i++) {
            ints[i] = (int) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8)  | ((blocks[i*2+2]&0xFF)<<16)  | ((blocks[i*2+3]&0xFF)<<24) );
        }
        return ints;
    }

    public boolean contains(final int x, final int y, final int z) {
        return this.bb.contains(x, y, z);
    }
    public boolean has(final int x, final int y, final int z) {
        int hash = TripletIntHash.toHash(x-this.bb.minX, y-this.bb.minY, z-this.bb.minZ);
        int idx = Arrays.binarySearch(this.blocks, hash);
        return idx >= 0;
    }

    public Iterator<BlockPos> iterator() {
        return new TreeBlockIterator(this, this.bb);
    }

    public Iterator<BlockPos> trunkIterator() {
        return new TreeBlockIterator(this, this.trunkBB);
    }

    public void onMine(BlockPlacer placer, Block blockLog, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        int data = w.getData(pos);
        int stage = data>>2;
        int type = data&0x3;
        stage++;

        if (stage >= 10) {
//            w.setType(pos.x, pos.y, pos.z, 0, Flags.MARK|Flags.LIGHT);
            long l = System.nanoTime();
            Iterator<BlockPos> it = trunkIterator();
            boolean done = true;
            BlockPos pos2 = new BlockPos();
            while (it.hasNext()) {
                BlockPos treeblock = it.next();
                int typeA = w.getType(treeblock);
                if (typeA == blockLog.id) {
                    boolean n2 = w.getData(treeblock) >> 2 < 9;
                    if (n2) {
                        boolean canBreak = false;
                        for (int i = 0; i < 6; i++) {
                            pos2.set(treeblock);
                            pos2.offset(i);
                            int n = w.getType(pos2);
                            if (Block.get(n).isReplaceable()) {
                                canBreak = true;
                                break;
                            }
                        }
                        if (canBreak) {
                            System.err.println("trunk block "+typeA+" is still log");
                            done = false;
                            break;
                        }
                    }
                }
            }
            if (done) {
                it = iterator();
                while (it.hasNext()) {
                    BlockPos treeblock = it.next();
                    if (treeblock.y < this.trunkBB.minY) {
                        if (w.getType(treeblock) == blockLog.id) {
                            if (w.getData(pos)==3) {
                                w.setData(treeblock.x, treeblock.y, treeblock.z, 0, Flags.MARK|Flags.LIGHT);
                            }
                            continue;
                        }
                    }
                    w.setType(treeblock.x, treeblock.y, treeblock.z, 0, Flags.MARK|Flags.LIGHT);
                }
                player.recvItem(new ItemStack(Item.wood));
            }
            l = System.nanoTime()-l;
            System.out.println(l/1000000L);
        } else {
            w.setData(pos.x, pos.y, pos.z, stage<<2|type, Flags.MARK|Flags.LIGHT);
        }
    }
}
