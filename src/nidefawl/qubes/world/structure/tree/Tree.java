package nidefawl.qubes.world.structure.tree;

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
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.structure.Structure;
import nidefawl.qubes.world.structure.StructureBlockIterator;

public class Tree extends Structure {
    public final AABBInt trunkBB = new AABBInt();
    public Tree() {
    }
    public Compound save() {
        Tag.Compound tag = super.save();
        Tag.Compound trunkBB = this.trunkBB.saveTag();
        tag.set("trunkBB", trunkBB);
        return tag;
    }

    public void load(Tag.Compound cmpTree) {
        super.load(cmpTree);
        Tag.Compound trunkBB = (Tag.Compound) cmpTree.get("trunkBB");
        this.trunkBB.loadTag(trunkBB);
    }

    public Iterator<BlockPos> trunkIterator() {
        return new StructureBlockIterator(this, this.trunkBB);
    }

    public void onMine(BlockPlacer placer, Block blockLog, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        int data = w.getData(pos);
        int stage = data>>2;
        stage++;
        if (stage < 10) {

            Iterator<BlockPos> it = trunkIterator();
            while (it.hasNext()) {
                BlockPos treeblock = it.next();
                int typeA = w.getType(treeblock);
                if (typeA == blockLog.id) {
                    int data2 = w.getData(treeblock);
                    int type2 = data2&0x3;
                    w.setData(treeblock.x, treeblock.y, treeblock.z, stage<<2|type2, Flags.MARK|Flags.LIGHT);
                }
            }
        }
        else {
//            w.setType(pos.x, pos.y, pos.z, 0, Flags.MARK|Flags.LIGHT);
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
                if (blockLog instanceof BlockLog) {
                    Item item = Item.log.getItem(((BlockLog)blockLog).getIndex());
                    if (item != null) {
                        player.recvItem(new ItemStack(item));
                    }
                }
            }
        }
    }
}
