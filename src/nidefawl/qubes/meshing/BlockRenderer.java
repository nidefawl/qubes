/**
 * 
 */
package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;

import java.util.Arrays;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.block.*;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.SingleBlockWorld;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class BlockRenderer {

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
        int multi = block.getTexturePasses();
        int n = 0;
        for (int pass = 0; pass < multi; pass++) {
            switch (renderType) {
                case 14:
                case 0: {
                    setDefaultBounds();
                    n += renderBlock(block, ix, iy, iz, pass, block.getLODPass()); //Normal block with default bounds 
                    break;
                }
                case 1:
                    n += renderPlant(block, ix, iy, iz, pass);
                    break;
                case 2: {
                    setBlockBounds(block, ix, iy, iz);
                    n += renderBlock(block, ix, iy, iz, pass, block.getLODPass()); //Normal block with custom bounds 
                    break;
                }
                case 3:
                    n += renderSlicedFaces((BlockSliced) block, ix, iy, iz);
                    break;
                case 4:
                    n += renderVines(block, ix, iy, iz, pass);
                    break;
                case 5:
                    n += renderFence(block, ix, iy, iz, pass);
                    break;
                case 6:
                    n += renderWall(block, ix, iy, iz, pass);
                    break;
                case 7:
                    n += renderDoublePlant(block, ix, iy, iz, pass);
                    break;
                case 8:
                    n += renderTorch(block, ix, iy, iz);
                    break;
                case 9:
                    n += renderPane(block, ix, iy, iz, pass);
                    break;
                case 11:
                    n += renderPlantFlat(block, ix, iy, iz, pass);
                    break;
                case 12:
                    n += renderWaterLily(block, ix, iy, iz, pass);
                    break;
                case 13:
                    setDefaultBounds();
                    n += renderBlockModel(block, ix, iy, iz, pass, block.getLODPass());
                    break;
            }
        }
        return n;
    }


    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @param pass
     * @return
     */
    private int renderWaterLily(Block block, int ix, int iy, int iz, int pass) {
        float yOffset = 0;
        int water = this.w.getWater(ix, iy, iz);
        

        if (water > 0) {
            if (this.w.getWater(ix, iy+1, iz) == 0) {
                yOffset = 0.3f;
            }
        }
        int n= 0;
        n+= renderPlantFlat(Block.pad, ix, iy, iz, pass);
        this.attr.yOff+=yOffset;
        n += renderPlant(block, ix, iy, iz, pass);
        this.attr.yOff-=yOffset;
        return n;
    }

    protected int setPaneConnections(IBlockWorld w, int ix, int iy, int iz, int[] b) {
        return BlockPane.setPaneConnections(w, ix, iy, iz, paneDir);
    }
    private int renderPane(Block block, int ix, int iy, int iz, int texturepass) {
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
        BlockSurface yTopSurface = getSingleBlockSurface(block, ix, iy-1, iz, axis, 0, false, this.qSurfacesS[0], texturepass);
        BlockSurface yBottomSurface = getSingleBlockSurface(block, ix, iy+1, iz, axis, 1, false, this.qSurfacesS[1], texturepass);
        float o = 1 / 64f;
        float o2 = 1 / 16f;
        if (zp || zn) {
            this.attr.setOffset(0.5f, 0, 0);
            int faceDir = 0 << 1 | 1;
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, 0, 1, false, this.bs, texturepass);
            setFaceColor(block, ix, iy, iz, faceDir, surface, texturepass);
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
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 0, yTopSurface, tex, texturepass);
                renderYPos(block, ix, iy - o*yp, iz);
                putBuffer(block, targetBuffer);
                flipFace();
                putBuffer(block, targetBuffer);
            }
            //  
            if (yn < 2) {
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 1, yBottomSurface, tex, texturepass);
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
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, 2, 1, false, this.bs, texturepass);
            setFaceColor(block, ix, iy, iz, faceDir, surface, texturepass);
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
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 0, yTopSurface, tex, texturepass);
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
                setFaceColorTexture(block, ix, iy, iz, axis << 1 | 1, yBottomSurface, tex, texturepass);
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
        int c = block.getFaceColor(w, ix, iy, iz, Dir.DIR_POS_Y, 0);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int data = this.w.getData(ix, iy, iz);
        int tex = block.getTexture(Dir.DIR_POS_Y, data, 0);
        
        attr.setAO(0);
        attr.setTex(tex);
        attr.setNormalMap(0);
        attr.setFaceDir(Dir.DIR_POS_Y);

        int br_pp = brigthness;
        int br_cp = brigthness;
        int br_pc = brigthness;
        int br_nc = brigthness;
        int br_np = brigthness;
        int br_nn = brigthness;
        int br_cn = brigthness;
        int br_pn = brigthness;
        int brPP = BlockFaceAttr.mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = BlockFaceAttr.mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = BlockFaceAttr.mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = BlockFaceAttr.mix_light(brigthness, br_pn, br_cn, br_pc);
        attr.maskLight(brNN, brPN, brPP, brNP, block.getLightValue());
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
    protected int renderWall(Block block, int ix, int iy, int iz, int texturepass) {
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
            f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
        } 

        this.bb.minY = 0;
        this.bb.maxY = this.bb.minY+(fenceHPx/16F);
        if (!hasPost) {
            this.bb.set(
                    fenceWStart*dir,         0,            fenceWStart*(1-dir), 
                    fenceWEnd*dir+(1-dir), this.bb.maxY, fenceWEnd*(1-dir)+dir );
            f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
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
                f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
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
     * @param texturepass 
     * @return
     */
    protected int renderFence(Block block, int ix, int iy, int iz, int texturepass) {
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
        f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
        
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
                f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
                this.bb.offset(0, -6/16f, 0);
                f += renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
            }
        }
        
        return f;
    }

    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @param texturepass 
     * @return
     */
    protected int renderVines(Block block, int ix, int iy, int iz, int texturepass) {
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
                BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, this.bs, texturepass);
                setFaceColor(block, ix, iy, iz, axis<<1|side, surface, texturepass);
                f+= renderFace(block, axis<<1|side, ix, iy, iz, targetBuffer);
                flipFace();
                attr.setPass(3);
                attr.flipNormal();
//                attr.setNormal(-attr.v0.normal[0], -attr.normal[1], -attr.normal[2]);
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
            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, this.bs, texturepass);
            setFaceColor(block, ix, iy, iz, axis<<1|side, surface, texturepass);
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
    protected void putSingleVert(Block block, int targetBuffer, int attrIdx) {
        attr.putSingleVert(attrIdx, this.vbuffer[targetBuffer]);
        if (block.getRenderShadow()>0) {
            if (this.shadowDrawMode == 1) {
                attr.putShadowTexturedSingleVert(attrIdx, this.vbuffer[PASS_SHADOW_SOLID]);
            } else {
                attr.putBasicSingleVert(attrIdx, this.vbuffer[PASS_SHADOW_SOLID]);
            }   
        }
    }

    protected void incVertCount(Block block, int targetBuffer, int vIdx) {
        this.vbuffer[targetBuffer].incVertCount(vIdx);
        if (block.getRenderShadow()>0) {
            this.vbuffer[PASS_SHADOW_SOLID].incVertCount(vIdx);
        }
    }

    protected void putTriIndex(Block block, int targetBuffer, int[] vertexIdx) {
        this.vbuffer[targetBuffer].putTriIndex(vertexIdx);
        if (block.getRenderShadow()>0) {
            this.vbuffer[PASS_SHADOW_SOLID].putTriIndex(vertexIdx);
        }
    }
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
                            if (qSurfaces[n] == null) {//STILL REQUIRED?!
                                qSurfaces[n] = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, qSurfacesS[n], 0);

                            }
                            Block textureBlock = block;
                            if (textureBlock instanceof BlockQuarterBlock) {
                                textureBlock = Block.get(quarters[q]);
                            }

                            int nSubTexturePasses = textureBlock.getTexturePasses();
                            for (int texPass = 0; texPass < nSubTexturePasses; texPass++) {
                
                                //TODO: figure out how to get cached getSingleBlockSurface for multipass textures
                                if (textureBlock.skipTexturePassSide(this.w, x, y, z, axis, side, texPass)) {
                                    continue;
                                }
                                int tex = textureBlock.getTexture(n, w.getData(ix, iy, iz), texPass);
                                setFaceColorTexture(textureBlock, ix, iy, iz, n, qSurfaces[n], tex, texPass);
                                f += renderFace(block, n, ix, iy, iz, targetBuffer);
                            }
                        }
                    }
                }
            }
        }

        extendFaces = true;
        return f;
    }
    private int renderPlantFlat(Block block, int ix, int iy, int iz, int texturepass) {
        int targetBuffer = block.getLODPass();
        int f = 0;
        this.bb.set(0, 0, 0, 1, 0.05f, 1);
        int water = this.w.getWater(ix, iy, iz);
        float yOffset = 0;

        if (water > 0) {
            if (this.w.getWater(ix, iy+1, iz) == 0) {
                yOffset = 0.3f;
            }
        }
        this.attr.yOff+=yOffset;
        setBlockBounds(block, ix, iy, iz);
        BlockSurface top = getSingleBlockSurface(block, ix, iy, iz, 1, 0, false, bs, texturepass);
        setFaceColor(block, ix, iy, iz, 1<<1|0, top, texturepass);
        final long multiplier = 0x5DEECE66DL;
        final long addend = 0xBL;
        final long mask = (1L << 48) - 1;
        long seed = (ix * 5591 + iy * 19 + iz * 7919);
        long iR = ((multiplier * seed + addend) & mask);
        float y=iy;
        if (this.w.getType(ix, iy-1, iz) == Block.water.id)
            y-=0.1f;
        attr.setNormal(0, 1, 0);// set upward normal
        //TODO: handle uv rotation
        attr.v0.setUV(1, 0);
        attr.v1.setUV(0, 0);
        attr.v2.setUV(0, 1);
        attr.v3.setUV(1, 1);
        
        attr.v0.setPos(ix + bb.maxX, y + bb.maxY, iz + bb.minZ);
        attr.v1.setPos(ix + bb.minX, y + bb.maxY, iz + bb.minZ);
        attr.v2.setPos(ix + bb.minX, y + bb.maxY, iz + bb.maxZ);
        attr.v3.setPos(ix + bb.maxX, y + bb.maxY, iz + bb.maxZ);
        attr.rotateUV((int) (iR & 3));
        putBuffer(block, targetBuffer);
        flipFace();
        putBuffer(block, targetBuffer);
        f+=2;
        this.attr.yOff-=yOffset;
        
        return f;
    }

    protected int renderBlock(Block block, int ix, int iy, int iz, int texturepass, int targetBuffer) {
        int f = 0;
        for (int n = 0; n < 6; n++) {
            f += getAndRenderBlockFace(block, ix, iy, iz, n/2, n%2, texturepass, targetBuffer);
        }
        return f;
    }
    private int renderBlockModel(Block block, int ix, int iy, int iz, int texturepass, int targetBuffer) {
        int f = 0;
        try {
            setDefaultBounds();
            ModelBlock model = block.getBlockModel(this.w, ix, iy, iz, texturepass);
            if (model == null) {
                return renderBlock(block, ix, iy, iz, texturepass, targetBuffer);
            }
            int vPos[] = new int[model.loader.listVertex.size()]; //could be much smaller (when vertex are grouped by face(group)
            int vIdx = 0;
            Arrays.fill(vPos, -1);
            for (int faceDir = 0; faceDir < 6; faceDir++) {
    //            int faceDir = Dir.DIR_POS_Y;
//                if (faceDir != Dir.DIR_NEG_X) continue;
                int axis = (faceDir)/2;
                int side = (faceDir)%2;
                final float vScale = 1/8f;
                BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, false, this.bs, texturepass);
                if (surface != null) {
                    //TODO: precalc this, cache, cache, cache!
                    
                    setFaceColor(block, ix, iy, iz, faceDir, surface, texturepass);
//                    f+= renderFace(block, faceDir, ix, iy, iz, targetBuffer);    
                    
    
                    QModelTriGroup group = model.groups[faceDir];
                    QModelMaterial mat = group.material;
                    int numTris = group.listTri.size();
//                    int vertexIdx[] = new int[numTris*3*2]; // could be smaller
                    final int faceCount = numTris/2;
                    int[] vertexIdx = new int[3];
                    for (int i = 0; i < numTris; i++) {
                        QModelTriangle tri = group.listTri.get(i);
                        int vIdxOut = 0;
                        for (int v = 0; v < 3; v++) {
                            int triVertIdx = tri.vertIdx[v];
                            if (vPos[triVertIdx] < 0) {
                                vPos[triVertIdx] = vIdx++;
                                QModelVertex vertex = model.loader.getVertex(tri.vertIdx[v]);
                                
                                attr.v[0].setNormal(tri.normal[v].x, tri.normal[v].y, tri.normal[v].z);
                                attr.v[0].setUV(tri.texCoord[0][v], tri.texCoord[1][v]);
                                attr.v[0].setPos(ix + vertex.x*vScale, iy + vertex.y*vScale, iz + vertex.z*vScale);
                                attr.setFaceDir(-1);
                                putSingleVert(block, targetBuffer, 0);
                            }
                            vertexIdx[vIdxOut] = vPos[triVertIdx];
                            vIdxOut++;
                        }
                        putTriIndex(block, targetBuffer, vertexIdx);
                    }
//                    putIndex(block, targetBuffer, vertexIdx, vIdxOut, faceCount);
                    
                    f+=faceCount;
                }

            
            }
            incVertCount(block, targetBuffer, vIdx);
        } catch (Exception e) {
            e.printStackTrace();
        }

////        renderYPos(block, ix, iy, iz);
//        putBuffer(block, targetBuffer);
//        for (int n = 0; n < 6; n++) {
//            int axis = n/2;
//            int side = n%2;
//            BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, true, this.bs, texturepass);
//            if (surface != null) {
//                int faceDir = axis<<1|side;
//                setFaceColor(block, ix, iy, iz, faceDir, surface, texturepass);
//                
//                f += renderFace(block, faceDir, ix, iy, iz, targetBuffer);    
//            }
//            
//        }
        return f;
        
    }

    float f(int x, int z) {
        float xf = (x)/8f;
        float zf = (z)/8f;
        if (xf>0.5f) {
            xf = 1-xf;
        }
        if (zf>0.5f) {
            zf = 1-zf;
        }
        xf /= 0.5f;
        zf /= 0.5f;
        float xzf = xf*zf;
        return xzf;
    }
    


    protected void flipFace() {
        int dir = attr.getFaceDir();
        if (dir < 0) {
            attr.setReverse(attr.getReverse());
        } else {
            int axis = dir>>1;
            int side = dir & 1;
            side = 1 - side;
            dir = axis << 1 | side;
            attr.setFaceDir(dir);
            attr.setReverse((side&1)!=0);
        }
    }

    protected void setFaceColorTexture(Block block, int ix, int iy, int iz, int faceDir, BlockSurface bs, int tex, int texturepass) {

        float alpha = block.getAlpha();
        int rgb = block.getFaceColor(w, ix, iy, iz, faceDir, texturepass);
        attr.setTex(tex);
        attr.setNormalMap(block.getNormalMap(tex));
        attr.setFaceDir(faceDir);
        attr.setReverse((bs.face&1)!=0);
        attr.setAO(bs.maskedAO);
        attr.setLight(bs.maskedLightSky, bs.maskedLightBlock);
        attr.setType(bs.type);
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBA(rgb, alpha);
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
    protected void setFaceColor(Block block, int ix, int iy, int iz, int faceDir, BlockSurface bs, int texturepass) {
        int tex = block.getTexture(faceDir, w.getData(ix, iy, iz), texturepass);
        setFaceColorTexture(block, ix, iy, iz, faceDir, bs, tex, texturepass);
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

    protected int getAndRenderBlockFace(Block block, int ix, int iy, int iz, int axis, int side, int texturepass, int targetBuffer) {
        BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, true, this.bs, texturepass);
        if (surface != null) {
            int faceDir = axis<<1|side;
            setFaceColor(block, ix, iy, iz, faceDir, surface, texturepass);
            return renderFace(block, faceDir, ix, iy, iz, targetBuffer);    
        }
        return 0;
    }
    
    //TODO: cache this by ix,iy,iz,axis,side (for stairs)
    protected BlockSurface getSingleBlockSurface(Block block, int ix, int iy, int iz, int axis, int side, boolean checkVisibility, BlockSurface out, int texturepass) {
        if (block.skipTexturePassSide(this.w, ix, iy, iz, axis, side, texturepass)) {
            return null;
        }
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
        if (w instanceof SingleBlockWorld) {
            out.maskedAO=BlockSurface.maskAO(2, 2, 2, 2);
        } else {
            out.calcAO(this.w);
        }
        return out;
    }


    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    private int renderDoublePlant(Block block, int ix, int iy, int iz, int pass) {
        return renderPlant(block, ix, iy, iz, pass);
    }
    
    Vector3f plantNormal = new Vector3f();
    int renderPlant(Block block, int ix, int iy, int iz, int pass) {
        int targetBuffer = block.getLODPass();
        int brigthness = this.w.getLight(ix, iy, iz);
        float alpha = block.getAlpha();
        int data = this.w.getData(ix, iy, iz);
        int tex = block.getTexture(Dir.DIR_POS_Y, data, pass);
        boolean isDoublePlant = block instanceof BlockDoublePlant;
        boolean bendNormal = !(block instanceof BlockDoublePlant) || (data&0x8) != 0;
        int topAO = !bendNormal?2:1;
        int bottomAO = !bendNormal?2:2;
        attr.setAO(BlockFaceAttr.maskAO(bottomAO,bottomAO,topAO,topAO));
        attr.setTex(tex);
        attr.setNormalMap(0);
        attr.setFaceDir(-1);

        int br_pp = brigthness;
        int br_cp = brigthness;
        int br_pc = brigthness;
        int br_nc = brigthness;
        int br_np = brigthness;
        int br_nn = brigthness;
        int br_cn = brigthness;
        int br_pn = brigthness;
        int brPP = BlockFaceAttr.mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = BlockFaceAttr.mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = BlockFaceAttr.mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = BlockFaceAttr.mix_light(brigthness, br_pn, br_cn, br_pc);
        attr.maskLight(brNN, brPN, brPP, brNP, block.getLightValue());
        attr.setType(block.id);
        attr.setNormal(0, 1, 0); // set upward normal


        float x = ix;
        float y = iy;
        float z = iz;
        float h = 1f;
        float w = 1f;
        float rot = 0.25f;
        int num = 2;
        if (isDoublePlant|| (block instanceof BlockPlantCrossedSquares && ((BlockPlantCrossedSquares)block).applyRandomOffset())) {
            int rotBits = isDoublePlant?4:2;
            
            final long multiplier = 0x5DEECE66DL;
            final long addend = 0xBL;
            final long mask = (1L << 48) - 1;
            int ySeed = isDoublePlant?88:iy;
            long seed = (ix * 5591 + ySeed * 19 + iz * 7919);
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
            if (!isDoublePlant)
            h = 0.5f+ fh*0.3f;
        }
        if (block.getTexturePasses() > 1) {
            if (pass == 2)
                num = 6;
            if (pass == 0 && block.getTexturePasses() > 1)
                num = 4;
        }

//        int rgb2 = Biome.MEADOW_GREEN.getFaceColor(BiomeColor.FOLIAGE2);
        int rgb2 = this.w.getBiomeFaceColor(ix, iy, iz, Dir.DIR_POS_Y, pass, BiomeColor.FOLIAGE2);
        float incr = 1/(float)num;
//        num = 2;
//        incr = 0.5f;
//        rot = 0;
        for (int i = 0; i < num; i++) {

            float frot = rot*GameMath.PI_OVER_180*180;
            float sin = GameMath.sin(frot);
            float cos = GameMath.cos(frot);
//            System.out.println("rot "+rot+" =" +sin+","+cos);

            float sideOffset = 1 - w;
            float sideTexOffset = 1 - Math.min(1, w);
            int rgb = block.getFaceColor(this.w, ix, iy, iz, i, pass);
            for (int v = 0; v < 4; v++) {
                attr.v[v].setColorRGB(rgb);
                attr.v[v].setFaceVertDir(0);
                attr.v[v].setNoDirection();
            }
            if (isDoublePlant) {
                {
                    int rgb3 = TextureUtil.mixRGB(rgb, rgb2, bendNormal ? 0.8f : 0.4f);
                    attr.v2.setColorRGB(rgb3);
                    attr.v1.setColorRGB(rgb3);
                }
                if (bendNormal)
                {
                    int rgb3 = TextureUtil.mixRGB(rgb, rgb2, 0.4f);
                    attr.v3.setColorRGB(rgb3);
                    attr.v0.setColorRGB(rgb3);
                }
            } else if ((rgb&0xFFFFFF) != 0xFFFFFF) {
                int rgb3 = TextureUtil.mixRGB(rgb, rgb2, 0.8f);
                attr.v1.setColorRGB(rgb3);
                attr.v2.setColorRGB(rgb3);
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
            
            if (bendNormal) {
                attr.calcNormal(plantNormal);
                plantNormal.y+=1.9f;
                plantNormal.normalise();
                int normal = attr.packNormal(plantNormal);
                for (int j = 0; j < 4; j++) {
                    attr.v1.normal = normal;
                    attr.v2.normal = normal;
                }
            }
            attr.v1.setPass(6);
            attr.v2.setPass(6);
            if (bendNormal && isDoublePlant) {
                attr.v1.setPass(7);
                attr.v2.setPass(7);
                attr.v0.setPass(6);
                attr.v3.setPass(6);
            }
            
            putBuffer(block, targetBuffer);
            attr.setReverse(true);
            if (bendNormal) {

                attr.calcNormal(plantNormal);
                plantNormal.y+=1.9f;
                plantNormal.normalise();

                int normal = attr.packNormal(plantNormal);
                 for (int j = 0; j < 4; j++) {
                     attr.v1.normal = normal;
                     attr.v2.normal = normal;
                 }
                
            }
            attr.v1.setPass(6);
            attr.v2.setPass(6);
            if (bendNormal && isDoublePlant) {
                attr.v1.setPass(7);
                attr.v2.setPass(7);
                attr.v0.setPass(6);
                attr.v3.setPass(6);
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
