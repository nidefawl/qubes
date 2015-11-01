/**
 * 
 */
package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.*;
import static nidefawl.qubes.render.region.RegionRenderer.REGION_SIZE_BLOCKS_MASK;
import static nidefawl.qubes.render.region.RegionRenderer.SLICE_HEIGHT_BLOCK_MASK;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.*;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class BlockRenderer {

    public static int mix_light(int br0, int br1, int br2, int br3) {
        // shift the upper nibble up by 4 bits so the overflow (bit 4-7) can be masked out later
        br0 = (br0 & 0xF) | (br0 & 0xF0) << 4;
        br1 = (br1 & 0xF) | (br1 & 0xF0) << 4;
        br2 = (br2 & 0xF) | (br2 & 0xF0) << 4;
        br3 = (br3 & 0xF) | (br3 & 0xF0) << 4;
        return (br0 + br1 + br2 + br3) >> 2;
    }

    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3 & 0x3) << 6) | ((ao2 & 0x3) << 4) | ((ao1 & 0x3) << 2) | (ao0 & 0x3);
    }
    final static int[][] offsets = new int[3*2][];
    static {
        for (int n = 0; n < 6; n++) {
            int axis = n / 2;
            int side = n % 2;
            int offx = axis == 0 ? 1-side*2 : 0;
            int offy = axis == 1 ? 1-side*2 : 0;
            int offz = axis == 2 ? 1-side*2 : 0;
            offsets[n] = new int[] {
                    offx, offy, offz
                };
        }
    }
    
    final BlockSurface       bs          = new BlockSurface();
//    private ChunkRenderCache ccache;
    final private AABBFloat  bb          = new AABBFloat();

    private int              shadowDrawMode;
    boolean                  extendFaces = true;

    protected IBlockWorld      w;
    protected BlockFaceAttr    attr;
    protected VertexBuffer[]   vbuffer;
    
    final boolean[]          wallDir     = new boolean[6];
    final boolean[]          fenceDir    = new boolean[4];
    final int[]              paneDir     = new int[6];
    final int[]              quarters    = new int[8];
    final int[]              quarters2   = new int[8];
    
    final AABBFloat[] boxes = new AABBFloat[] {
            new AABBFloat(0, 0, 0, 0.5f, 0.5f, 0.5f),
            new AABBFloat(0.5f, 0, 0, 1f, 0.5f, 0.5f),
            new AABBFloat(0.5f, 0, 0.5f, 1f, 0.5f, 1f),
            new AABBFloat(0, 0, 0.5f, 0.5f, 0.5f, 1f),
            new AABBFloat(0, 0.5f, 0, 0.5f, 1f, 0.5f),
            new AABBFloat(0.5f, 0.5f, 0, 1f, 1f, 0.5f),
            new AABBFloat(0.5f, 0.5f, 0.5f, 1f, 1f, 1f),
            new AABBFloat(0, 0.5f, 0.5f, 0.5f, 1f, 1f),
    };
    final BlockSurface[] qSurfacesS = new BlockSurface[] {
            new BlockSurface(),
            new BlockSurface(),
            new BlockSurface(),
            new BlockSurface(),
            new BlockSurface(),
            new BlockSurface(),
    };
    final BlockSurface[] qSurfaces = new BlockSurface[6];
    
    protected void setDefaultBounds() {
        bb.set(0, 0, 0, 1, 1, 1);
    }

    /**
     * @param w
     * @param ccache
     */
    protected void preRender(World w, ChunkRenderCache ccache, BlockFaceAttr attr) {
        this.w = w;
        this.attr = attr;
        this.extendFaces = true;
    }
    /**
     * @param vbuffer
     * @param shadowDrawMode2
     */
    protected void setBuffers(VertexBuffer[] vbuffer, int shadowDrawMode) {
        this.vbuffer = vbuffer;
        this.shadowDrawMode = shadowDrawMode;
    }

    protected void maskLight(int ao0, int ao1, int ao2, int ao3, Block block) {
        int sky = 0;
        sky |= (ao3 >> 8) & 0xF;// shift down by 8, skylight is now in upper byte (mix_light shifted it there), then mask out the overflow
        sky <<= 4;
        sky |= (ao2 >> 8) & 0xF;
        sky <<= 4;
        sky |= (ao1 >> 8) & 0xF;
        sky <<= 4;
        sky |= (ao0 >> 8) & 0xF;
        int self = block.getLightValue();
        int blockLight = 0;
        blockLight |= Math.max((ao3) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao2) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao1) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao0) & 0xF, self);
        attr.setLight(sky, blockLight);
    }

    public int render(int ix, int iy, int iz) {
        //        ccache
//        int i = ix & REGION_SIZE_BLOCKS_MASK;
//        int j = iy & SLICE_HEIGHT_BLOCK_MASK;
//        int k = iz & REGION_SIZE_BLOCKS_MASK;
        if (iy < 0 || iy >= World.MAX_WORLDHEIGHT) {
            System.err.println("j == " + iy + " should not happen");
            return 0;
        }
//        int chunkX = i >> Chunk.SIZE_BITS;
//        int chunkZ = k >> Chunk.SIZE_BITS;
//        Chunk chunk = ccache.get(chunkX, chunkZ);
//        if (chunk == null) {
//            System.err.println("CHUNK IS NULL should not happen");
//            return 0;
//        }
        int type = w.getType(ix, iy, iz);
        if (type < 1) {
            //happens when chunk data gets changed by main thread
            System.err.println("type == " + type + " should not happen (" + ix + "," + iy + "," + iz + ")");
            return 0;
        }
        //        light = chunk.getTypeId(i & 0xF, iy, k & 0xF);
        Block block = Block.get(type);
        int renderType = block.getRenderType();
        switch (renderType) {
            case 0: {
                setDefaultBounds();
                return renderBlock(block, ix, iy, iz, block.getLODPass()); //Normal block with default bounds 
            }
            case 1:
                return renderPlant(block, ix, iy, iz);
            case 2: {
                setBlockBounds(block, ix, iy, iz);
                return renderBlock(block, ix, iy, iz, block.getLODPass()); //Normal block with custom bounds 
            }
            case 3:
                return renderSlicedFaces((BlockSliced) block, ix, iy, iz);
            case 4:
                return renderVines(block, ix, iy, iz);
            case 5:
                return renderFence(block, ix, iy, iz);
            case 6:
                return renderWall(block, ix, iy, iz);
            case 7:
                return renderDoublePlant(block, ix, iy, iz);
            case 8:
                return renderTorch(block, ix, iy, iz);
            case 9:
                return renderPane(block, ix, iy, iz);
            case 11:
                return renderPlantFlat(block, ix, iy, iz);
        }
        return 0;
    }

    protected int setPaneConnections(IBlockWorld w, int ix, int iy, int iz, int[] b) {
        return BlockPane.setPaneConnections(w, ix, iy, iz, paneDir);
    }
    private int renderPane(Block block, int ix, int iy, int iz) {
        int f = 0;
        int targetBuffer = block.getLODPass();
        int tex = block.getTextureByIdx(1);
        int n = setPaneConnections(this.w, ix, iy, iz, paneDir);
        
        if (n == 0) {
            for (int i = 0; i < 2; i++) {
                paneDir[i] = 2;
                paneDir[2<<1|i] = 2;
            }
        }
        float minX = 1;
        float minZ = 1;
        float maxX = 0;
        float maxZ = 0;

        boolean zp = paneDir[Dir.DIR_POS_Z] > 0;
        boolean zn = paneDir[Dir.DIR_NEG_Z] > 0;
        int yp = paneDir[Dir.DIR_POS_Y];
        int yn = paneDir[Dir.DIR_NEG_Y];
        int axis = 1;
        BlockSurface yTopSurface = getSingleBlockSurface(block, ix, iy-1, iz, axis, 0, false, this.qSurfacesS[0]);
        BlockSurface yBottomSurface = getSingleBlockSurface(block, ix, iy+1, iz, axis, 1, false, this.qSurfacesS[1]);
        float o = 1 / 64f;
        float o2 = 1 / 16f;
        if (zp || zn) {
            this.attr.setOffset(0.5f, 0, 0);
            int faceDir = 0 << 1 | 1;
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, 0, 1, false, this.bs);
            setFaceColor(block, ix, iy, iz, faceDir, surface);
            setDefaultBounds();
            if (!zn) {
                this.bb.minZ = 0.5f;
            }
            if (!zp) {
                this.bb.maxZ = 0.5f;
            }
            f += renderFace(block, faceDir, ix, iy, iz, targetBuffer);
            flipFace();
            putBuffer(block, targetBuffer);
            f++;
            this.attr.setOffset(0, 0, 0);
            this.bb.minX = 0.5f-o2;
            this.bb.maxX = 0.5f+o2;

            //
            if (yp < 2) {
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 0, yTopSurface, tex);
                renderYPos(block, ix, iy - o*yp, iz);
                putBuffer(block, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
            }
            //  
            if (yn < 2) {
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 1, yBottomSurface, tex);
                renderYNeg(block, ix, iy + o*yn, iz);
                putBuffer(block, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
            }

            f += 4;
            minX = Math.min(this.bb.minX, minX);
            minZ = Math.min(this.bb.minZ, minZ);
            maxX = Math.max(this.bb.maxX, maxX);
            maxZ = Math.max(this.bb.maxZ, maxZ);
        }
        boolean xp = paneDir[Dir.DIR_POS_X] > 0;
        boolean xn = paneDir[Dir.DIR_NEG_X] > 0;
        if (xp || xn) {
            this.attr.setOffset(0, 0, 0.5f);
            int faceDir = 2 << 1 | 1;
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, 2, 1, false, this.bs);
            setFaceColor(block, ix, iy, iz, faceDir, surface);
            setDefaultBounds();
            if (!xn) {
                this.bb.minX = 0.5f;
            }
            if (!xp) {
                this.bb.maxX = 0.5f;
            }
            f += renderFace(block, faceDir, ix, iy, iz, targetBuffer);
            flipFace();
            putBuffer(block, targetBuffer);
            f++;
            this.attr.setOffset(0, 0, 0);
            this.bb.minZ = 0.5f-o2;
            this.bb.maxZ = 0.5f+o2;
            if (yp < 2) {
                float zOff = xn ? 0.5f : 0;
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 0, yTopSurface, tex);
                renderYPos(block, ix, iy - o*yp, iz);
                attr.rotateUV(3);
                attr.v[0].setUV(this.bb.minZ, zOff);
                attr.v[1].setUV(this.bb.minZ, zOff + 0.5f);
                attr.v[2].setUV(this.bb.maxZ, zOff + 0.5f);
                attr.v[3].setUV(this.bb.maxZ, zOff);
                putBuffer(block, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
            }
            if (yn < 2) {
                float zOff = xn ? 0.5f : 0;
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 1, yBottomSurface, tex);
                renderYNeg(block, ix, iy + o*yn, iz);
                attr.rotateUV(3);
                attr.v[0].setUV(this.bb.minZ, zOff);
                attr.v[1].setUV(this.bb.minZ, zOff + 0.5f);
                attr.v[2].setUV(this.bb.maxZ, zOff + 0.5f);
                attr.v[3].setUV(this.bb.maxZ, zOff);
                putBuffer(block, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
            }

            f += 4;

            minX = Math.min(this.bb.minX, minX);
            minZ = Math.min(this.bb.minZ, minZ);
            maxX = Math.max(this.bb.maxX, maxX);
            maxZ = Math.max(this.bb.maxZ, maxZ);
        }
        
        this.attr.setOffset(0, 0, 0);
    
        return f;
    }

    private int renderTorch(Block block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        int brigthness = this.w.getLight(ix, iy, iz);
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getFaceColor(w, ix, iy, iz, Dir.DIR_POS_Y);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int data = this.w.getData(ix, iy, iz);
        int tex = block.getTexture(Dir.DIR_POS_Y, data);
        
        attr.setAO(0);
        attr.setTex(tex);
        attr.setFaceDir(Dir.DIR_POS_Y);

        int br_pp = brigthness;
        int br_cp = brigthness;
        int br_pc = brigthness;
        int br_nc = brigthness;
        int br_np = brigthness;
        int br_nn = brigthness;
        int br_cn = brigthness;
        int br_pn = brigthness;
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, block);
        attr.setType(block.id);
        attr.setNormal(0, 1, 0); // set upward normal

        

        float h = 1f;
        float w = 1f;
        float xoff = 0f;
        float zoff = 0f;
        float yoff = iy+0.2f;
        switch (data) {
            default:
                yoff = iy;
                break;
            case 1:
                xoff = -0.5f;
                break;
            case 2:
                xoff = 0.5f;
                break;
            case 3:
                zoff = -0.5f;
                break;
            case 4:
                zoff = 0.5f;
                break;
        }
        float x = ix;
        float z = iz;
        float x2 = ix+xoff;
        float z2 = iz+zoff;
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
            attr.v[v].setFaceVertDir(0);
            attr.v[v].setNoDirection();
        }
        
        for (int i = 0; i < 4; i++)
        {
            int ax = (i&2)>>1;
            int side = i&1;
            float tmin=0.33f;
            float tmax = 1-tmin;
            attr.setOffset((0.5f+0.0625f*(-1+2*side))*(1-ax), 0, (0.5f+0.0625f*(-1+2*side))*(ax));
            int ax2=1-ax;
            attr.v0.setUV(tmin, 0);
            attr.v0.setPos(x2+tmin*ax, yoff, z2+tmin*ax2);
            attr.v1.setUV(tmin, h);
            attr.v1.setPos(x+tmin*ax, yoff + h, z+tmin*ax2);
            attr.v2.setUV(tmax, h);
            attr.v2.setPos(x+tmax*ax, yoff + h, z+tmax*ax2);
            attr.v3.setUV(tmax, 0);
            attr.v3.setPos(x2+tmax*ax, yoff, z2+tmax*ax2);
            attr.setReverse(side==ax);
            putBuffer(block, targetBuffer);
            attr.setReverse(side!=ax);
            putBuffer(block, targetBuffer);
        }
        {
            x2 = ix + xoff*(1-0.625f);
            z2 = iz + zoff*(1-0.625f);
            float tmin=0.45f;
            float tmax = 1-tmin;
            float t = 0.0625f;
            h = 0.625f;
            float to = 0.05f;
            attr.setOffset(0.5f, 0, 0.5f);
            attr.v1.setUV(tmin, tmin+to);
            attr.v1.setPos(x2 - t, yoff + h, z2 + t);
            attr.v0.setUV(tmin, tmax+to);
            attr.v0.setPos(x2 - t, yoff + h, z2 - t);
            attr.v3.setUV(tmax, tmax+to);
            attr.v3.setPos(x2 + t, yoff + h, z2 - t);
            attr.v2.setUV(tmax, tmin+to);
            attr.v2.setPos(x2 + t, yoff + h, z2 + t);
            attr.setReverse(false);
            putBuffer(block, targetBuffer);
            attr.setOffset(0, 0, 0);
        }
        return 4*2+1;
    }

    protected int setWallConnections(IBlockWorld w, int ix, int iy, int iz, boolean[] b) {
        return BlockWall.setWallConnections(w, ix, iy, iz, wallDir);
    }
    protected int renderWall(Block block, int ix, int iy, int iz) {
        int f = 0;
        int targetBuffer = block.getLODPass();
        int n = setWallConnections(this.w, ix, iy, iz, wallDir);

        float fencePostPx = 8;
        float fenceWPx = 6F;
        float fenceHPx = 13F;
        float postStart = (16-fencePostPx)/(32f);
        float postEnd = 1-postStart;
        float fenceWStart = (16-fenceWPx)/32F;
        float fenceWEnd = 1-fenceWStart;
        
        boolean hasPost = true;
        boolean xP = wallDir[Dir.DIR_POS_X];
        boolean xN = wallDir[Dir.DIR_NEG_X];
        boolean zP = wallDir[Dir.DIR_POS_Z];
        boolean zN = wallDir[Dir.DIR_NEG_Z];

        int dir = 0;
        if (xP&&xN&&!zP&&!zN) {
            hasPost = n != 2;
        } else if (!xP&&!xN&&zP&&zN) {
            hasPost = n != 2;
            dir = 1;
        }
        if (hasPost) {
            this.bb.set(postStart, 0, postStart, postEnd, 1, postEnd);
            f += renderBlock(block, ix, iy, iz, targetBuffer);
        } 

        this.bb.minY = 0;
        this.bb.maxY = this.bb.minY+(fenceHPx/16F);
        if (!hasPost) {
            this.bb.set(
                    fenceWStart*dir,         0,            fenceWStart*(1-dir), 
                    fenceWEnd*dir+(1-dir), this.bb.maxY, fenceWEnd*(1-dir)+dir );
            f += renderBlock(block, ix, iy, iz, targetBuffer);
        }
        for (int i = 0; hasPost && i < 6; i++) {
            if (wallDir[i]) {
                switch (i) {
                    case 0:
                        // to pos x
                        this.bb.minX = postEnd;
                        this.bb.maxX = 1;
                        this.bb.minZ = fenceWStart;
                        this.bb.maxZ = fenceWEnd;
                        break;
                    case 1:
                        //neg x
                        this.bb.minX = 0;
                        this.bb.maxX = postStart;
                        this.bb.minZ = fenceWStart;
                        this.bb.maxZ = fenceWEnd;
                        break;
                    case 2:
                    case 3:
                        continue;
                    case 4:
                        //pos z
                        this.bb.minZ = postEnd;
                        this.bb.maxZ = 1;
                        this.bb.minX = fenceWStart;
                        this.bb.maxX = fenceWEnd;
                        break;
                    case 5:
                        // neg z
                        this.bb.minZ = 0;
                        this.bb.maxZ = postStart;
                        this.bb.minX = fenceWStart;
                        this.bb.maxX = fenceWEnd;
                        break;
                }
                f += renderBlock(block, ix, iy, iz, targetBuffer);
            }
        }
        return f;
    }
    protected int setFenceConnections(IBlockWorld w, int ix, int iy, int iz, boolean[] b) {
        return BlockFence.setFenceConnections(w, ix, iy, iz, fenceDir);
    }
    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    protected int renderFence(Block block, int ix, int iy, int iz) {
        int f = 0;
        int targetBuffer = block.getLODPass();
        int n = setFenceConnections(this.w, ix, iy, iz, fenceDir);

        float fencePostPx = 4;
        float fenceWPx = 2F;
        float fenceHPx = 3F;
        float postStart = (16-fencePostPx)/(32f);
        float postEnd = 1-postStart;
        float fenceWStart = (16-fenceWPx)/32F;
        float fenceWEnd = 1-fenceWStart;
        this.bb.set(postStart, 0, postStart, postEnd, 1, postEnd);
        f += renderBlock(block, ix, iy, iz, targetBuffer);
        
        for (int i = 0; i < 4; i++) {
            if (fenceDir[i]) {
                this.bb.minY = 12/16F;
                this.bb.maxY = this.bb.minY+(fenceHPx/16F);
                switch (i) {
                    case 0:
                        // to pos x
                        this.bb.minX = postEnd;
                        this.bb.maxX = 1;
                        this.bb.minZ = fenceWStart;
                        this.bb.maxZ = fenceWEnd;
                        break;
                    case 1:
                        //neg x
                        this.bb.minX = 0;
                        this.bb.maxX = postStart;
                        this.bb.minZ = fenceWStart;
                        this.bb.maxZ = fenceWEnd;
                        break;
                    case 2:
                        //pos z
                        this.bb.minZ = postEnd;
                        this.bb.maxZ = 1;
                        this.bb.minX = fenceWStart;
                        this.bb.maxX = fenceWEnd;
                        break;
                    case 3:
                        // neg z
                        this.bb.minZ = 0;
                        this.bb.maxZ = postStart;
                        this.bb.minX = fenceWStart;
                        this.bb.maxX = fenceWEnd;
                        break;
                }
                f += renderBlock(block, ix, iy, iz, targetBuffer);
                this.bb.offset(0, -6/16f, 0);
                f += renderBlock(block, ix, iy, iz, targetBuffer);
            }
        }
        
        return f;
    }

    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    protected int renderVines(Block block, int ix, int iy, int iz) {
        int f = 0;
        int targetBuffer = block.getLODPass();
        setBlockBounds(block, ix, iy, iz);
        int data = this.w.getData(ix, iy, iz);
        float thickness = 0.05f;
        float minOffset = 1/256f;
        for (int j = 0; j < 4; j++) {
            int bit = 1<<j;
            int axis, side;
            if ((data & bit) != 0) {
                switch (j) {
                    default:
                    case 2:
                        //neg z
                        this.bb.set(0, 0, 0, 1, 1, thickness);
                        this.bb.offset(0, 0, minOffset);
                        axis = 2; side = 0;
                        break;
                    case 1:
                        //pos x
                        this.bb.set(0, 0, 0, thickness, 1, 1);
                        this.bb.offset(minOffset, 0, 0);
                        axis = 0; side = 0;
                        break;
                    case 0:
                        //pos z
                        this.bb.set(0, 0, 1-thickness, 1, 1, 1);
                        this.bb.offset(0, 0, -minOffset);
                        axis = 2; side = 1;
                        break;
                    case 3:
                        //neg x
                        this.bb.set(1-thickness, 0, 0, 1, 1, 1);
                        this.bb.offset(-minOffset, 0, 0);
                        axis = 0; side = 1;
                        break;
                }
                BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, this.bs);
                setFaceColor(block, ix, iy, iz, axis<<1|side, surface);
                f+= renderFace(block, axis<<1|side, ix, iy, iz, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
                f++;
            }
        }
        int typeAbove = this.w.getType(ix, iy+1, iz);
        Block b = Block.get(typeAbove);
        if (b.isNormalBlock(w, ix, iy+1, iz)) {
            int axis, side;
            axis = 1; side = 1;
            this.bb.set(0, 1-thickness, 0, 1, 1, 1);
            this.bb.offset(0, -minOffset, 0);
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, this.bs);
            setFaceColor(block, ix, iy, iz, axis<<1|side, surface);
            f+= renderFace(block, axis<<1|side, ix, iy, iz, targetBuffer);
//            flipFace();
//            putBuffer(block, targetBuffer);
//            f++;
        }
        return f;
    }

    protected void renderXNeg(Block block, float x, float y, float z) {
        attr.setNormal(-1, 0, 0);
        attr.v2.setUV(bb.maxZ, bb.maxY);
        attr.v3.setUV(bb.maxZ, bb.minY);
        attr.v0.setUV(bb.minZ, bb.minY);
        attr.v1.setUV(bb.minZ, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
    }

    protected void renderXPos(Block block, float x, float y, float z) {
        attr.setNormal(1, 0, 0);
        attr.v0.setUV(1-bb.minZ, bb.minY);
        attr.v1.setUV(1-bb.minZ, bb.maxY);
        attr.v2.setUV(1-bb.maxZ, bb.maxY);
        attr.v3.setUV(1-bb.maxZ, bb.minY);
        attr.v0.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
    }
    protected void renderZNeg(Block block, float x, float y, float z) {
        attr.setNormal(0, 0, -1);
        attr.v0.setUV(1-bb.minX, bb.maxY);
        attr.v1.setUV(1-bb.minX, bb.minY);
        attr.v2.setUV(1-bb.maxX, bb.minY);
        attr.v3.setUV(1-bb.maxX, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v2.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
    }
    protected void renderZPos(Block block, float x, float y, float z) {
        attr.setNormal(0, 0, 1);
        attr.v0.setUV(bb.minX, bb.maxY);
        attr.v1.setUV(bb.minX, bb.minY);
        attr.v2.setUV(bb.maxX, bb.minY);
        attr.v3.setUV(bb.maxX, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
        attr.v2.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
    }
    protected void renderYPos(Block block, float x, float y, float z) {
        attr.setNormal(0, 1, 0);// set upward normal
        //TODO: handle uv rotation
        attr.v0.setUV(bb.maxX, bb.minZ);
        attr.v1.setUV(bb.minX, bb.minZ);
        attr.v2.setUV(bb.minX, bb.maxZ);
        attr.v3.setUV(bb.maxX, bb.maxZ);
        
        attr.v0.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
    }


    protected void renderYNeg(Block block, float x, float y, float z) {
        attr.setNormal(0, -1, 0);// set downward normal
        attr.v0.setUV(bb.maxX, bb.minZ);
        attr.v1.setUV(bb.minX, bb.minZ);
        attr.v2.setUV(bb.minX, bb.maxZ);
        attr.v3.setUV(bb.maxX, bb.maxZ);
        attr.v0.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
    }
    
    protected int renderFace(Block block, int faceDir, float x, float y, float z, int targetBuffer) {
        switch (faceDir) {
            case 1:
                renderXNeg(block, x, y, z);
                break;
            case 0:
                renderXPos(block, x, y, z);
                break;
            case 3:
                renderYNeg(block, x, y, z);
                break;
            case 2:
                renderYPos(block, x, y, z);
                break;
            case 5:
                renderZNeg(block, x, y, z);
                break;
            case 4:
                renderZPos(block, x, y, z);
                break;
        }
        putBuffer(block, targetBuffer);
        return 1;
    }


    /**
     * @param block 
     * @param targetBuffer 
     * 
     */
    protected void putBuffer(Block block, int targetBuffer) {
        attr.put(this.vbuffer[targetBuffer]);
        if (block.getRenderShadow()>0) {
            if (this.shadowDrawMode == 1) {
                attr.putShadowTextured(this.vbuffer[PASS_SHADOW_SOLID]);
            } else {
                attr.putBasic(this.vbuffer[PASS_SHADOW_SOLID]);
            }   
        }
    }

    protected int renderSlicedFaces(BlockSliced block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        int f = 0;
        extendFaces = false;
        block.getQuarters(this.w, ix, iy, iz, quarters);
        for (int n = 0; n < 6; n++) {
            qSurfaces[n] = null;
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int q = y*4+z*2+(z>0?1-x:x);
                    if (quarters[q] > 0) {
                        this.bb.set(boxes[q]);
                        for (int n = 0; n < 6; n++) {
                            int axis = n / 2;
                            int side = n % 2;
                            int offset[] = offsets[n];
                            int offx = x + offset[0];
                            int offy = y + offset[1];
                            int offz = z + offset[2];
                            int wX = 0;
                            int wY = 0;
                            int wZ = 0;
                            if (offx < 0) {
                                wX--;
                                offx += 2;
                            } else if (offx > 1) {
                                wX++;
                                offx -= 2;
                            }
                            if (offz < 0) {
                                wZ--;
                                offz += 2;
                            } else if (offz > 1) {
                                wZ++;
                                offz -= 2;
                            }
                            if (offy < 0) {
                                wY--;
                                offy += 2;
                            } else if (offy > 1) {
                                wY++;
                                offy -= 2;
                            }
                            int[] adjArr = quarters;
                            if (wX != 0 || wY != 0 || wZ != 0) {
                                Block nextFull = Block.get(this.w.getType(ix+wX, iy+wY, iz+wZ));
                                nextFull.getQuarters(this.w, ix+wX, iy+wY, iz+wZ, quarters2);
                                adjArr = quarters2;
                            }
                            int qInnerAdjIdx = offy*4+offz*2+(offz>0?1-offx:offx);
                            int nAdjId = adjArr[qInnerAdjIdx];
                            
                            if (!Block.get(nAdjId).isTransparent()) {
                                continue;
                            }
                            if (qSurfaces[n] == null) {
                                qSurfaces[n] = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, qSurfacesS[n]);
                            }
                            Block textureBlock = block;
                            if (textureBlock instanceof BlockQuarterBlock) {
                                textureBlock = Block.get(quarters[q]);
                            }
                            int tex = textureBlock.getTexture(n, w.getData(ix, iy, iz));
                            setFaceColorTexture(block, ix, iy, iz, n, qSurfaces[n], tex);
                            f += renderFace(block, n, ix, iy, iz, targetBuffer);
                        }
                    }
                }
            }
        }

        extendFaces = true;
        return f;
    }
    private int renderPlantFlat(Block block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        int f = 0;
        this.bb.set(0, 0, 0, 1, 0.05f, 1);
        BlockSurface top = getSingleBlockSurface(block, ix, iy, iz, 1, 0, false, bs);
        setFaceColor(block, ix, iy, iz, 1<<1|0, top);
        final long multiplier = 0x5DEECE66DL;
        final long addend = 0xBL;
        final long mask = (1L << 48) - 1;
        long seed = (ix * 5591 + iy * 19 + iz * 7919);
        long iR = ((multiplier * seed + addend) & mask);
        float y=iy;
        if (this.w.getType(ix, iy-1, iz) == Block.water.id)
            y-=0.1f;
        renderYPos(block, ix, y, iz);
        attr.rotateUV((int) (iR & 3));
        putBuffer(block, targetBuffer);
        flipFace();
        putBuffer(block, targetBuffer);
        f+=2;
        
        return f;
    }

    protected int renderBlock(Block block, int ix, int iy, int iz, int targetBuffer) {
        int f = 0;
        for (int n = 0; n < 6; n++) {
            f += getAndRenderBlockFace(block, ix, iy, iz, n/2, n%2, targetBuffer);
        }
        return f;
    }

    /**
     * @param n
     */

    protected void flipFace() {
        int dir = attr.getFaceDir();
        int axis = dir>>1;
        int side = dir & 1;
        side = 1 - side;
        dir = axis << 1 | side;
        attr.setFaceDir(dir);
        attr.setReverse((side&1)!=0);
    }
    protected void setFaceColorTexture(Block block, int ix, int iy, int iz, int faceDir, BlockSurface bs, int tex) {
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getFaceColor(w, ix, iy, iz, faceDir);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;
        attr.setTex(tex);
        attr.setFaceDir(faceDir);
        attr.setReverse((bs.face&1)!=0);
        attr.setAO(bs.maskedAO);
        attr.setLight(bs.maskedLightSky, bs.maskedLightBlock);
        attr.setType(bs.type);
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
//            if (extendFaces) {
//                int idx = v;
//                if (faceDir/2>0) {
//                    idx = (idx + 3) % 4;
//                }
//                attr.v[v].setFaceVertDir(faceVDirections[faceDir][idx]);
//                attr.v[v].setDirection(faceDir, idx, false);
//            } else {
                attr.v[v].setNoDirection();
//            }
        }
        
    }
    protected void setFaceColor(Block block, int ix, int iy, int iz, int faceDir, BlockSurface bs) {
        int tex = block.getTexture(faceDir, w.getData(ix, iy, iz));
        setFaceColorTexture(block, ix, iy, iz, faceDir, bs, tex);
    }

    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     */
    protected void setBlockBounds(Block block, int ix, int iy, int iz) {
        AABBFloat f = block.getRenderBlockBounds(w, ix, iy, iz, this.bb);
        if (f == null) {
            setDefaultBounds();
        }
    }

    protected int getAndRenderBlockFace(Block block, int ix, int iy, int iz, int axis, int side, int targetBuffer) {
        BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, true, this.bs);
        if (surface != null) {
            int faceDir = axis<<1|side;
            setFaceColor(block, ix, iy, iz, faceDir, surface);
            return renderFace(block, faceDir, ix, iy, iz, targetBuffer);    
        }
        return 0;
    }
    
    //TODO: cache this by ix,iy,iz,axis,side (for stairs)
    protected BlockSurface getSingleBlockSurface(Block block, int ix, int iy, int iz, int axis, int side, boolean checkVisibility, BlockSurface out) {
        if (checkVisibility) {
            int offx = axis == 0 ? 1-side*2 : 0;
            int offy = axis == 1 ? 1-side*2 : 0;
            int offz = axis == 2 ? 1-side*2 : 0;
            int neighbour = this.w.getType(ix+offx, iy+offy, iz+offz);
            Block b = Block.get(neighbour);
            if (b != null && !b.isFaceVisible(this.w, ix+offx, iy+offy, iz+offz, axis, side, block, bb)) {
                return null;
            }
        }
        out.x = ix;
        out.y = iy;
        out.z = iz;
        out.axis = axis;
        out.face = side;
        out.type = block.id;
        out.texture = 0;
        out.transparent = block.isTransparent();
        out.pass = block.getRenderPass();
        out.extraFace = false;
        out.calcLight = true;
        out.isLeaves = false;
        out.calcAO(this.w);
        return out;
    }


    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    private int renderDoublePlant(Block block, int ix, int iy, int iz) {
        return renderPlant(block, ix, iy, iz);
    }
    int renderPlant(Block block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        int brigthness = this.w.getLight(ix, iy, iz);
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getFaceColor(w, ix, iy, iz, Dir.DIR_POS_Y);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int tex = block.getTexture(Dir.DIR_POS_Y, this.w.getData(ix, iy, iz));
        
        attr.setAO(0);
        attr.setTex(tex);
        attr.setFaceDir(Dir.DIR_POS_Y);

        int br_pp = brigthness;
        int br_cp = brigthness;
        int br_pc = brigthness;
        int br_nc = brigthness;
        int br_np = brigthness;
        int br_nn = brigthness;
        int br_cn = brigthness;
        int br_pn = brigthness;
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, block);
        attr.setType(block.id);
        attr.setNormal(0, 1, 0); // set upward normal


        float x = ix;
        float y = iy;
        float z = iz;
        float h = 1f;
        float w = 1f;
        float rot = 0.25f;
        int num = 2;
        if (block instanceof BlockPlantCrossedSquares && ((BlockPlantCrossedSquares)block).applyRandomOffset()) {
            int rotBits = 2;
            
            final long multiplier = 0x5DEECE66DL;
            final long addend = 0xBL;
            final long mask = (1L << 48) - 1;
            long seed = (ix * 5591 + iy * 19 + iz * 7919);
            long iR = ((multiplier * seed + addend) & mask);
            float fR = 0.6F;
            int n = 12;
            int ma = (1 << n) - 1;
            x += ((iR & ma) / (float) (ma)) * fR - 0.5f * fR;
            z += (((iR >> n) & ma) / (float) (ma)) * fR - 0.5f * fR;
            int wBits = (int) ((iR>>4)&0x7);
            float fw = wBits / 7.0f;
            w = 0.9f+ fw*0.2f;

            int rBits = (int) ((iR>>7)&((1<<rotBits)-1));
            rot = rBits / ((float)(1<<rotBits));
            int nBits = (int) ((iR>>15)&0x3);
            int nBits2 = (int) ((iR>>17)&0x3);
            nBits &= nBits2;
            num+=nBits;
            int hBits = (int) ((iR>>22)&0x3);
            float fh = hBits / 3.0f;
            h = 0.5f+ fh*0.3f;
        }
        float incr = 1/(float)num;
        for (int i = 0; i < num; i++) {

            float frot = rot*GameMath.PI_OVER_180*180;
            float sin = GameMath.sin(frot);
            float cos = GameMath.cos(frot);
//            System.out.println("rot "+rot+" =" +sin+","+cos);

            float sideOffset = 1 - w;
            float sideTexOffset = 1 - Math.min(1, w);
            for (int v = 0; v < 4; v++) {
                attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
                attr.v[v].setFaceVertDir(0);
                attr.v[v].setNoDirection();
            }
            sideOffset  = 0;
            sideTexOffset = 0;
            float halfw = w/2.0f;
            float minX = 0.5f-halfw*sin;
            float maxX = 0.5f+halfw*sin;
            float minZ = 0.5f-halfw*cos;
            float maxZ = 0.5f+halfw*cos;
            minX+=x;
            maxX+=x;
            minZ+=z;
            maxZ+=z;
            attr.v0.setUV(sideTexOffset, 0);
            attr.v0.setPos(minX + sideOffset, y, minZ + sideOffset);

            attr.v1.setUV(sideTexOffset, 1);
            attr.v1.setPos(minX + sideOffset, y + h, minZ + sideOffset);

            attr.v2.setUV(1 - sideTexOffset, 1);
            attr.v2.setPos(maxX - sideOffset, y + h, maxZ - sideOffset);

            attr.v3.setUV(1 - sideTexOffset, 0);
            attr.v3.setPos(maxX - sideOffset, y, maxZ - sideOffset);
            
            
            attr.setReverse(false);
            float nup=0.9f;
            float nside=0.3f;
            float ny=nup;
            {
                float nx=nside;
                float nz=-nside;
                attr.v1.setNormal(nx, ny, nz); // set upward normal
                attr.v2.setNormal(nx, ny, nz); // set upward normal
            }
            
            putBuffer(block, targetBuffer);
            
            attr.setReverse(true);
            {
                float nx=-nside;
                float nz=nside;
                attr.v1.setNormal(nx, ny, nz); // set upward normal
                attr.v2.setNormal(nx, ny, nz); // set upward normal
            }
            

            putBuffer(block, targetBuffer);
            rot+=incr;
        }
//
//        attr.v0.setUV(sideTexOffset, 0);
//        attr.v0.setPos(x + 1 - sideOffset, y, z + sideOffset);
//
//        attr.v1.setUV(sideTexOffset, h);
//        attr.v1.setPos(x + 1 - sideOffset, y + h, z + sideOffset);
//
//        attr.v2.setUV(1 - sideTexOffset, h);
//        attr.v2.setPos(x + sideOffset, y + h, z + 1 - sideOffset);
//
//        attr.v3.setUV(1 - sideTexOffset, 0);
//        attr.v3.setPos(x + sideOffset, y, z + 1 - sideOffset);
//
//        attr.setReverse(false);
//        {
//            float nx=nside;
//            float nz=nside;
//            attr.v1.setNormal(nx, ny, nz); // set upward normal
//            attr.v2.setNormal(nx, ny, nz); // set upward normal
//        }
//        
//
//        putBuffer(block, targetBuffer);
//        attr.setReverse(true);
//        {
//            float nx=-nside;
//            float nz=-nside;
//            attr.v1.setNormal(nx, ny, nz); // set upward normal
//            attr.v2.setNormal(nx, ny, nz); // set upward normal
//        }
//
//
//        putBuffer(block, targetBuffer);
        return 4;
    }

}
