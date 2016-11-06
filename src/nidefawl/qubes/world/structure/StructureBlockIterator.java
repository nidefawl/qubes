package nidefawl.qubes.world.structure;

import java.util.ArrayList;
import java.util.Iterator;

import nidefawl.qubes.util.TripletIntHash;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.structure.tree.Tree;

public class StructureBlockIterator implements Iterator<BlockPos> {
    final Structure tree;
    private int pos;
    private int[] blocks;

    BlockPos bPos = new BlockPos();
    private AABBInt bb;
    public StructureBlockIterator(Structure tree, AABBInt bb) {
        this.pos = 0;
        this.tree = tree;
        this.bb = bb;
        if (this.bb != tree.bb) {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i < tree.blocks.length; i++) {
                int ipos = tree.blocks[i];
                bPos.x = TripletIntHash.getX(ipos)+tree.bb.minX;
                bPos.y = TripletIntHash.getY(ipos)+tree.bb.minY;
                bPos.z = TripletIntHash.getZ(ipos)+tree.bb.minZ;
                if (this.bb.contains(bPos)) {
                    list.add(ipos);
                }
            }
            this.blocks = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                this.blocks[i] = list.get(i);
            }
        } else {
            this.blocks = tree.blocks;
        }
    }

    @Override
    public boolean hasNext() {
        return pos < blocks.length;
    }

    @Override
    public BlockPos next() {
        int ipos = blocks[pos++];
        bPos.x = TripletIntHash.getX(ipos)+tree.bb.minX;
        bPos.y = TripletIntHash.getY(ipos)+tree.bb.minY;
        bPos.z = TripletIntHash.getZ(ipos)+tree.bb.minZ;
        return bPos;
    }

    @Override
    public void remove() {
    }
}