package nidefawl.qubes.util;

import java.util.ArrayList;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.world.World;

public class CollisionQuery {
    ArrayList<BlockColl> collisions = new ArrayList<BlockColl>();
    int numCollisions = 0;
    final AABB tmpBB = new AABB(0, 0, 0, 0, 0, 0);
    public CollisionQuery() {
    }

    private BlockColl get() {
        while (this.collisions.size() < this.numCollisions+1) {
            this.collisions.add(new BlockColl());
        }
        return this.collisions.get(this.numCollisions);
    }
    public void query(World world, AABB aabb) {
        this.numCollisions = 0;
        int minX = GameMath.floor(aabb.minX)-1;
        int minY = GameMath.floor(aabb.minY)-1;
        int minZ = GameMath.floor(aabb.minZ)-1;
        int maxX = GameMath.floor(aabb.maxX+1)+1;
        int maxY = GameMath.floor(aabb.maxY+1)+1;
        int maxZ = GameMath.floor(aabb.maxZ+1)+1;
        if (minY < 0) minY = 0;
        if (maxY < 0) {//out of world
            return;
        }
        int nBlocks = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                

                for (int y = minY; y <= maxY; y++) {
                    nBlocks++;
                    int type = world.getType(x, y, z);
                    Block b = Block.get(type);
                    if (b != null) {
                        AABB bb = b.getCollisionBB(world, x, y, z, tmpBB);
                        if (bb != null) {
                            if (bb.intersects(aabb)) {
                                BlockColl coll = get();
                                coll.blockBB.set(bb);
                                coll.type = type;
                                coll.x = x;
                                coll.y = y;
                                coll.z = z;
                                numCollisions++;
                            }
                        }
                        
                    }
                }
            }
        }
//        System.err.println(nBlocks);
    }
    
    public int getNumCollisions() {
        return numCollisions;
    }
    public BlockColl get(int i) {
        return this.collisions.get(i);
    }
}
