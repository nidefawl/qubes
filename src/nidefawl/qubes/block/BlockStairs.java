/**
 * 
 */
package nidefawl.qubes.block;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.item.Stack;
import nidefawl.qubes.meshing.SlicedBlockFaceInfo;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockStairs extends BlockSliced {

    /**
     * @param id
     * @param transparent
     */
    final Block baseBlock;
    private final int overrideTextureIdx;
    static boolean isUpsideDown(int a) {
        return (a & 0x4) != 0;
    }
    public BlockStairs(int id, Block baseBlock) {
        this(id, baseBlock, -1);
    }
    public BlockStairs(int id, Block baseBlock, int textureIdx) {
        super(id, baseBlock.isTransparent());
        setTextures(new String[0]);
        this.baseBlock = baseBlock;
        this.overrideTextureIdx = textureIdx;
    }

    @Override
    public boolean isOccluding() {
        return this.baseBlock.isOccluding();
    }

    @Override
    public int getLightValue() {

        return this.baseBlock.getLightValue();
    }

    @Override
    public boolean applyAO() {
        return this.baseBlock.applyAO();
    }

    @Override
    public boolean isTransparent() {
        return this.baseBlock.isTransparent();
    }

    @Override
    public int getBBs(World w, int ix, int iy, int iz, AABBFloat[] bb) {
        int data = w.getData(ix, iy, iz);
        int type = BlockStairs.stairTypeAt(w, ix, iy, iz);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        AABBFloat bb1 = bb[0];
        AABBFloat bb2 = bb[1];
        bb1.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (bottomTop == 1) {
            bb1.offset(0, 0.5f, 0);
        }
        setStairBB(bb2, rot, bottomTop, type, 1);
        bb1.offset(ix, iy, iz);
        bb2.offset(ix, iy, iz);
        if (type>0&&type>>1<2) {
            AABBFloat bb3 = bb[2];
            setStairBB(bb3, rot, bottomTop, type, 2);
            bb3.offset(ix, iy, iz);
            return 3;
        }
        return 2;
    }
    @Override
    public boolean raytrace(RayTrace rayTrace, World world, int x, int y, int z, Vector3f origin, Vector3f direction, Vector3f dirFrac) {
        int data = world.getData(x, y, z);
        int type = BlockStairs.stairTypeAt(world, x, y, z);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        AABBFloat bb1 = rayTrace.getTempBB();
        bb1.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (bottomTop == 1) {
            bb1.offset(0, 0.5f, 0);
        }
        bb1.offset(x, y, z);
        boolean b = bb1.raytrace(rayTrace, origin, direction, dirFrac);
        setStairBB(bb1, rot, bottomTop, type, 1);
        bb1.offset(x, y, z);
        b |= bb1.raytrace(rayTrace, origin, direction, dirFrac);
        if (type>0&&type>>1<2) {
            setStairBB(bb1, rot, bottomTop, type, 2);
            bb1.offset(x, y, z);
            b |= bb1.raytrace(rayTrace, origin, direction, dirFrac);
        }
        return b;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return this.baseBlock.getFaceColor(w, x, y, z, faceDir, pass);
    }

    @Override
    public float getAlpha() {
        return this.baseBlock.getAlpha();
    }

    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        int data = w.getData(ix, iy, iz)&0x3;
        bb.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (data == 1) {
            bb.offset(0, 0.5f, 0);
        } else if (data == 2) {
            bb.set(0, 0, 0, 1, 1, 1);
        }
        return bb;
    }


    @Override
    public int prePlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        int sData = 0;
        if (offset != Dir.DIR_POS_Y && (offset == Dir.DIR_NEG_Y || (fpos.y%1.0f) >= 0.5f)) // placed against a bottom face
            sData = 4;
        int rot = blockPlacer.getPlayer().getLookDir();
        int rotdata = 0;
        switch (rot) {
            case Dir.DIR_NEG_X:
                rotdata = 1;
                break;
            case Dir.DIR_POS_X:
                rotdata = 3;
                break;
            case Dir.DIR_NEG_Z:
                rotdata = 2;
                break;
            case Dir.DIR_POS_Z:
                rotdata = 0;
                break;
        }
        return sData|rotdata;
    }

    @Override
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        if (isVisibleBounds(w, axis, side, bb)) {
            return true;
        }

        int data = w.getData(ix, iy, iz);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        if (axis == 1) {
            return bottomTop != side;
        } else {
            int t = BlockStairs.stairTypeAt(w, ix, iy, iz);
            if (rot == 3 && axis == 2)
                return side == 1 || (t>0&&(t>>1)>1);
            if (rot == 1 && axis == 2) {
                return side == 0 || (t>0&&(t>>1)>1);
            }
            if (rot == 0 && axis == 0)
                return side == 0 || (t>0&&(t>>1)>1);
            if (rot == 2 && axis == 0)
                return side == 1 || (t>0&&(t>>1)>1);
        }
        return true;
    }
    @Override
    public boolean isNormalBlock(IBlockWorld w, int ix, int iy, int iz) {
//      int data = w.getData(ix, iy, iz) & 0x3;
        //return data == 2;
        return false;
    }
    /**
     * @return
     */
    public boolean isStairs() {
        return true;
    }
    
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idx = overrideTextureIdx;
        if (idx >= 0) {
            return BlockTextureArray.getInstance().getTextureIdx(baseBlock.id, idx);
        }
        if (this.textures.length == 0)
            return baseBlock.getTexture(faceDir, 0, pass);
        return BlockTextureArray.getInstance().getTextureIdx(this.id, Dir.isTopBottom(faceDir) ? 1 : 0);
    }
    public boolean isFullBB() {
        return false;
    }

    final static int[] offsetXZ = new int[] {
            -1,0,
            0,-1,
            1,0,
            0,1
    };
    final static int[] offsetXZ2 = new int[] {
            0,1,
            1,0,
            0,-1,
            -1,0
    };
    public static int stairTypeAt(IBlockWorld w, int x, int y, int z) {
        int data = w.getData(x, y, z);
        int rot = data & 0x3;
        boolean thisUpsideDown = isUpsideDown(data);
        int stairtype = 0;
        for (int a = 0; stairtype == 0 && a < 2; a++) {
            int idx = ((rot+a*2)%4)*2;
            int offx = offsetXZ[idx];
            int offz = offsetXZ[idx+1];
            int sttype = w.getType(x+offx, y, z+offz);
            Block b = Block.get(sttype);
            if (b!=null&&b.isStairs()) {
                int stdata = w.getData(x+offx, y, z+offz);
                if (isUpsideDown(stdata) == thisUpsideDown) {
                    stdata &= 0x3;
                    for (int r = 0; stairtype == 0 && r < 2; r++) {
                        int rotn = ((rot+1+r*2)&3);
                        
                        if (stdata==rotn) {
                            int dir = 1-2*r;
                            if (rotn%2==0)
                                dir = -dir;
//                            if (rot/2==0)
//                                dir = -dir;
                            int dx = x+(-offz)*dir;
                            int dz = z+(-offx)*dir;
                            Block n = Block.get(w.getType(dx, y, dz));
                            if (n == null || !n.isStairs() || (w.getData(dx, y, dz)) != data)
                                stairtype = (r+2*a)<<1|1;
                        }
                    }
                }
            }
        }
        return (stairtype);
    }
    
    public void getQuarters(IBlockWorld w, int x, int y, int z, int[] quarters) {
        int data = w.getData(x, y, z);
        int type = BlockStairs.stairTypeAt(w, x, y, z);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        Arrays.fill(quarters, 0);
        int offset = 4 * (bottomTop);
        for (int i = offset; i < offset + 4; i++) {
            quarters[i] = this.id;
        }
        
        // QUARTER INDEXING (one y slice)
        /*
         *   ___________
         *  |     |     |
         *  |  0  |  1  |
         *  |_____|_____|
         *  |     |     |
         *  |  3  |  2  |
         *  |_____|_____|
         *  
         *  idx +1 or -1 is always the neighbor
         *  so idx 0 + 3 == idx 0 - 1 
         *  
         */
        offset = 4 - offset;
        int stairType = (type==0)?0:1+((type>>1)&3);
        int xPos = 1-(rot%2);
        int zPos = rot/2;
        int idx0 = (1-xPos)+(1-zPos)*2;
        int idx1 = idx0+3;
        idx1 %= 4;
        int idx2 = idx1+3;
        idx2 %= 4;
        int idx3 = idx1+2;
        idx3 %= 4;
        switch (stairType) {
            case 4:
                quarters[offset+idx1] = this.id;
                break;
            case 3:
                quarters[offset+idx0] = this.id;
                break;
            case 2:
                quarters[offset+idx2] = this.id;
                quarters[offset+idx0] = this.id;
                quarters[offset+idx1] = this.id;
                break;
            case 1:
                quarters[offset+idx3] = this.id;
                quarters[offset+idx0] = this.id;
                quarters[offset+idx1] = this.id;
                break;
            case 0:
                quarters[offset+idx0] = this.id;
                quarters[offset+idx1] = this.id;
                break;
        }
    }
    /**
     * @param bb
     * @param rot
     * @param topBottom
     * @param i
     */
    public static void setStairBB(AABBFloat bb, int rot, int topBottom, int type, int i) {
        float min = 0F;
        float max = 1F;
        float min2 = 0F;
        float max2 = 0.5F;
        if (type > 0) {
            int t1 = (type>>1);
            if (t1 < 2 && i > 1) {
                int trot = t1&1;
                int lrot = rot % 2;
                if (rot / 2 == trot) {
                    lrot = 1-lrot;
                }
                min2 = -0.5f+(rot/2);
                max2 = min2+0.5f;
                min = 0.5f*(lrot);
                if (i>2)
                    min=0.5f-min;
                max = min +0.5f;
            } else if (t1 >= 2) {
                int trot = t1-2;
                int lrot = rot%2;
                if (rot / 2 == trot) {
                    lrot = 1-lrot;
                }
                if (i == 4) {
                    min = 0;
                    max = 1;
                    min2 = -0.5f+(rot/2);
                    max2 = min2+0.5f;
                } else {
                    min += 0.5f*(lrot);
                    max -= 0.5f*(1-lrot);
                }
                if (i==3) {
                    min=0.5f-min;
                    max = min +0.5f;
                }
            }
        }
        
        switch (rot) {
            case 2:
                bb.set(min2, 0, min, max2, 0.5f, max);
                break;
            case 3:
                bb.set(min, 0, min2, max, 0.5f, max2);
                break;
            case 0:
                bb.set(0.5f+min2, 0, min, 0.5f+max2, 0.5f, max);
                break;
            default:
            case 1:
                bb.set(min, 0, 0.5f+min2, max, 0.5f, 0.5f+max2);
                break;
        }
        if (topBottom == 0) {
            bb.minY+=0.5f;
            bb.maxY+=0.5f;   
        }
    }
    public boolean isOccludingBlock(IBlockWorld w, int x, int y, int z) {
        return false;
    }
}
