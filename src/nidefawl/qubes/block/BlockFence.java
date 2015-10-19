/**
 * 
 */
package nidefawl.qubes.block;

import java.util.List;

import nidefawl.qubes.item.Stack;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockFence extends Block {
    private Block baseBlock;
    
    public static int setFenceConnections(IBlockWorld w, int ix, int iy, int iz, boolean[] b) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            int axis = i & 2;
            int side = i & 1;
            b[i] = canConnectTo(w, ix, iy, iz, axis, side);
            if (b[i]) {
                n++;
            }
        }
        return n;
    }
    
    public static boolean canConnectTo(IBlockWorld w, int ix, int iy, int iz, int axis, int side) {
        int faceDir = axis<<1|side;
        ix+=Dir.getDirX(faceDir);
        iy+=Dir.getDirY(faceDir);
        iz+=Dir.getDirZ(faceDir);
        int type = w.getType(ix, iy, iz);
        Block block = Block.get(type);
        return block.canBlockConnect(w, ix, iy , iz, BlockConnect.FENCE, axis, 1-side);
    }
    @Override
    public boolean canBlockConnect(IBlockWorld w, int ix, int iy, int iz, BlockConnect connect, int axis, int i) {
        return connect == BlockConnect.FENCE;
    }

    public BlockFence(int id, Block baseBlock) {
        super(id, true);
        this.textures = NO_TEXTURES;
        this.baseBlock = baseBlock;
    }


    @Override
    public int getLightValue() {

        return this.baseBlock.getLightValue();
    }

    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir) {
        return this.baseBlock.getFaceColor(w, x, y, z, faceDir);
    }

    @Override
    public float getAlpha() {
        return this.baseBlock.getAlpha();
    }
    @Override
    public int getTexture(int faceDir, int dataVal) {
        int axis = faceDir>>1;
        if (axis == 1) {
            faceDir = 0|faceDir&1;
        }
        if (this.textures.length == 0)
            return baseBlock.getTexture(faceDir, dataVal);
        return super.getTexture(faceDir, dataVal);
    }
    @Override
    public int getRenderType() {
        return 5;
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
    public int getRenderShadow() {
        return 1;
    }
    
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        float fencePostPx = 4;
        float fenceWPx = 2F;
        float fenceHPx = 3F;
        float postStart = (16-fencePostPx)/(32f);
        float postEnd = 1-postStart;
        float fenceWStart = (16-fenceWPx)/32F;
        float fenceWEnd = 1-fenceWStart;
        bb.set(postStart, 0, postStart, postEnd, 1, postEnd);
        if (canConnectTo(w, ix, iy, iz, 0, 0))
            bb.maxX = 1;
        if (canConnectTo(w, ix, iy, iz, 0, 1))
            bb.minX = 0;
        if (canConnectTo(w, ix, iy, iz, 2, 0))
            bb.maxZ = 1;
        if (canConnectTo(w, ix, iy, iz, 2, 1))
            bb.minZ = 0;
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
