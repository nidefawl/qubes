/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockPane extends Block {
    
    public static int setPaneConnections(IBlockWorld w, int ix, int iy, int iz, int[] b) {
        int n = 0;
        for (int i = 0; i < 6; i++) {
            int axis = i >> 1;
            int side = i & 1;
            b[i] = getConnect(w, ix, iy, iz, axis, side);
            if (axis != 1 && b[i] > 0) {
                n++;
            }
        }
        return n;
    }
    
    private boolean canConnectTo(IBlockWorld w, int ix, int iy, int iz, int axis, int side) {
        return getConnect(w, ix, iy, iz, axis, side) > 0;
    }

    public static int getConnect(IBlockWorld w, int ix, int iy, int iz, int axis, int side) {
        int faceDir = axis<<1|side;
        ix+=Dir.getDirX(faceDir);
        iy+=Dir.getDirY(faceDir);
        iz+=Dir.getDirZ(faceDir);
        int type = w.getType(ix, iy, iz);
        Block block = Block.get(type);
        return block instanceof BlockPane ? 2 : (block.canBlockConnect(w, ix, iy , iz, BlockConnect.PANE, axis, 1-side) ? 1 : 0);
    }
    @Override
    public boolean canBlockConnect(IBlockWorld w, int ix, int iy, int iz, BlockConnect connect, int axis, int i) {
        return connect == BlockConnect.PANE;
    }
    
    public BlockPane(String id) {
        super(id, true);
    }
    @Override
    public int getRenderType() {
        return 9;
    }
    
    
    @Override
    public int getRenderShadow() {
        return 0;
    }
    
    @Override
    public boolean isOccluding() {
        return false;
    }

    @Override
    public boolean isFullBB() {
        return false;
    }

    @Override
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        return true;
    }


    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        bb.set(0.5f, 0, 0.5f, 0.5f, 1, 0.5f);
        int connX = 0;
        int connZ = 0;
        if (canConnectTo(w, ix, iy, iz, 0, 0)) {
            bb.maxX = 1;
            connX++;
        }
        if (canConnectTo(w, ix, iy, iz, 0, 1)) {
            bb.minX = 0;
            connX++;
        }
        if (canConnectTo(w, ix, iy, iz, 2, 0)) {
            bb.maxZ = 1;
            connZ++;
        }
        if (canConnectTo(w, ix, iy, iz, 2, 1)) {
            bb.minZ = 0;
            connZ++;
        }
        final float o = 1/16.0f;
        if (connX+connZ == 0) {
            bb.set(0, 0, 0, 1, 1, 1);
        } else if (connX == 0) {
            bb.minX-=o;
            bb.maxX+=o;
        } else if (connZ == 0) {
            bb.minZ-=o;
            bb.maxZ+=o;
        }
        return bb;
    }

    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        float fencePostPx = 4;
        float postStart = (16-fencePostPx)/(32f);
        float postEnd = 1-postStart;
        tmp[0].set(postStart, 0, postStart, postEnd, 1, postEnd);
        boolean xP = canConnectTo(world, x, y, z, 0, 0);
        boolean xN = canConnectTo(world, x, y, z, 0, 1);
        boolean zP = canConnectTo(world, x, y, z, 2, 0);
        boolean zN = canConnectTo(world, x, y, z, 2, 1);
        if (xP&&xN&&!zP&&!zN) {
            if (xN)
                tmp[0].minX = 0;
            if (xP)
                tmp[0].maxX = 1;
            tmp[0].offset(x, y, z);
            return 1;
        }
        if (!xP&&!xN&&zP&&zN) {
            if (zN)
                tmp[0].minZ = 0;
            if (zP)
                tmp[0].maxZ = 1;
            tmp[0].offset(x, y, z);
            return 1;
        }
        int idx = 1;
        if (xP) {
            AABBFloat bb = tmp[idx++];
            bb.set(tmp[0]);
            bb.minX = postEnd;
            bb.maxX = 1;
        }
        if (xN) {
            AABBFloat bb = tmp[idx++];
            bb.set(tmp[0]);
            bb.minX = 0;
            bb.maxX = postStart;
        }
        if (zP) {
            AABBFloat bb = tmp[idx++];
            bb.set(tmp[0]);
            bb.minZ = postEnd;
            bb.maxZ = 1;
        }
        if (zN) {
            AABBFloat bb = tmp[idx++];
            bb.set(tmp[0]);
            bb.minZ = 0;
            bb.maxZ = postStart;
        }
        for (int i = 0; i < idx; i++) {
            tmp[i].offset(x, y, z);
        }
        return idx;
    }
}
