/**
 * 
 */
package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.*;
import static nidefawl.qubes.render.region.RegionRenderer.REGION_SIZE_BLOCKS_MASK;
import static nidefawl.qubes.render.region.RegionRenderer.SLICE_HEIGHT_BLOCK_MASK;

import nidefawl.qubes.block.*;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class BlockRenderer {
    final static int[][] faceVDirections = BlockFace.faceVDirections;
    final BlockSurface bs = new BlockSurface();
    private ChunkRenderCache ccache;
    private World            w;
    private BlockFaceAttr    attr;
    
    final private AABBFloat bb = new AABBFloat();
    
    private int shadowDrawMode;
    
    boolean extendFaces = true;
    private VertexBuffer[] vbuffer;
    
    public void setDefaultBounds() {
        bb.set(0, 0, 0, 1, 1, 1);
    }

    /**
     * @param w
     * @param ccache
     */
    public void preRender(World w, ChunkRenderCache ccache, BlockFaceAttr attr) {
        this.w = w;
        this.ccache = ccache;
        this.attr = attr;
        this.extendFaces = true;
    }
    /**
     * @param vbuffer
     * @param shadowDrawMode2
     */
    public void setBuffers(VertexBuffer[] vbuffer, int shadowDrawMode) {
        this.vbuffer = vbuffer;
        this.shadowDrawMode = shadowDrawMode;
    }

    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3 & 0x3) << 6) | ((ao2 & 0x3) << 4) | ((ao1 & 0x3) << 2) | (ao0 & 0x3);
    }

    public static int mix_light(int br0, int br1, int br2, int br3) {
        // shift the upper nibble up by 4 bits so the overflow (bit 4-7) can be masked out later
        br0 = (br0 & 0xF) | (br0 & 0xF0) << 4;
        br1 = (br1 & 0xF) | (br1 & 0xF0) << 4;
        br2 = (br2 & 0xF) | (br2 & 0xF0) << 4;
        br3 = (br3 & 0xF) | (br3 & 0xF0) << 4;
        return (br0 + br1 + br2 + br3) >> 2;
    }

    public void maskLight(int ao0, int ao1, int ao2, int ao3, Block block) {
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

    int getLight(int ix, int iy, int iz) {
        int i = ix & REGION_SIZE_BLOCKS_MASK;
        int k = iz & REGION_SIZE_BLOCKS_MASK;
        int chunkX = i >> Chunk.SIZE_BITS;
        int chunkZ = k >> Chunk.SIZE_BITS;
        Chunk chunk = ccache.get(chunkX, chunkZ);
        return chunk.getLight(i & 0xF, iy, k & 0xF);
    }

    public int render(int ix, int iy, int iz) {
        //        ccache
        int i = ix & REGION_SIZE_BLOCKS_MASK;
        int j = iy & SLICE_HEIGHT_BLOCK_MASK;
        int k = iz & REGION_SIZE_BLOCKS_MASK;
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            System.err.println("j == " + j + " should not happen");
            return 0;
        }
        int chunkX = i >> Chunk.SIZE_BITS;
        int chunkZ = k >> Chunk.SIZE_BITS;
        Chunk chunk = ccache.get(chunkX, chunkZ);
        if (chunk == null) {
            System.err.println("CHUNK IS NULL should not happen");
            return 0;
        }
        int type = chunk.getTypeId(i & 0xF, iy, k & 0xF);
        if (type < 1) {
            //happens when chunk data gets changed by main thread
            System.err.println("type == " + type + " should not happen (" + i + "," + j + "," + k + " - " + ix + "," + iy + "," + iz + ")");
            return 0;
        }
        //        light = chunk.getTypeId(i & 0xF, iy, k & 0xF);
        Block block = Block.get(type);
        int renderType = block.getRenderType();
        switch (renderType) {
            case 1:
                return renderPlant(block, ix, iy, iz);
            case 2:
                return renderBlock(block, ix, iy, iz); //Normal block with custom bounds 
            case 3:
                return renderSlicedFaces((BlockSliced) block, ix, iy, iz);
        }
        return 0;
    }


    void renderXNeg(Block block, float x, float y, float z) {
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

    void renderXPos(Block block, float x, float y, float z) {
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
    void renderZNeg(Block block, float x, float y, float z) {
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
    void renderZPos(Block block, float x, float y, float z) {
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
    void renderYPos(Block block, float x, float y, float z) {
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


    void renderYNeg(Block block, float x, float y, float z) {
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
    
    int renderFace(Block block, int faceDir, float x, float y, float z, int targetBuffer) {
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
        attr.put(this.vbuffer[targetBuffer]);
        if (block.getRenderShadow()>0) {
            if (this.shadowDrawMode == 1) {
                attr.putShadowTextured(this.vbuffer[PASS_SHADOW_SOLID]);
            } else {
                attr.putBasic(this.vbuffer[PASS_SHADOW_SOLID]);
            }   
        }
        return 1;
    }


    final int[] quarters = new int[8];
    final int[] quarters2 = new int[8];
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
    private int renderSlicedFaces(BlockSliced block, int ix, int iy, int iz) {
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
                            setFaceColor(block, qSurfaces[n], n);
                            f += renderFace(block, n, ix, iy, iz, targetBuffer);
                        }
                    }
                }
            }
        }

        extendFaces = true;
        return f;
    }

    int renderBlock(Block block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        setBlockBounds(block, ix, iy, iz);
        int f = 0;
        for (int n = 0; n < 6; n++) {
            f += getAndRenderBlockFace(block, ix, iy, iz, n/2, n%2, targetBuffer);
        }
        return f;
    }

    /**
     * @param n
     */
    private void setFaceColor(Block block, BlockSurface bs, int faceDir) {
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColorFromSide(faceDir);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;
        int tex = block.getTexture(faceDir, 0);
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

    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     */
    private void setBlockBounds(Block block, int ix, int iy, int iz) {
        AABBFloat f = block.getRenderBlockBounds(w, ix, iy, iz, this.bb);
        if (f == null) {
            setDefaultBounds();
        }
    }

    private int getAndRenderBlockFace(Block block, int ix, int iy, int iz, int axis, int side, int targetBuffer) {
        BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side, true, this.bs);
        if (surface != null) {
            int faceDir = axis<<1|side;
            setFaceColor(block, surface, faceDir);
            return renderFace(block, faceDir, ix, iy, iz, targetBuffer);    
        }
        return 0;
    }
    
    //TODO: cache this by ix,iy,iz,axis,side (for stairs)
    private BlockSurface getSingleBlockSurface(Block block, int ix, int iy, int iz, int axis, int side, boolean checkVisibility, BlockSurface out) {
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
        int data = this.w.getData(ix, iy, iz); //TODO: this is queried multiple times
        out.x = ix & REGION_SIZE_BLOCKS_MASK;
        out.y = iy;
        out.z = iz & REGION_SIZE_BLOCKS_MASK;
        out.axis = axis;
        out.face = side;
        out.type = block.id;
        out.data = data;
        out.transparent = block.isTransparent();
        out.pass = block.getRenderPass();
        out.extraFace = false;
        out.calcLight = true;
        out.isLeaves = false;
        out.calcAO(this.ccache);
        return out;
    }

    int renderPlant(Block block, int ix, int iy, int iz) {
        int targetBuffer = block.getLODPass();
        int brigthness = getLight(ix, iy, iz);
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColorFromSide(Dir.DIR_POS_Y);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int tex = block.getTexture(Dir.DIR_POS_Y, this.bs.data);
        
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

        final long multiplier = 0x5DEECE66DL;
        final long addend = 0xBL;
        final long mask = (1L << 48) - 1;

        float x = ix;
        float y = iy;
        float z = iz;
        if (block instanceof BlockPlantCrossedSquares && ((BlockPlantCrossedSquares)block).applyRandomOffset()) {
            long seed = (ix * 5591 + iy * 19 + iz * 7919);
            long iR = ((multiplier * seed + addend) & mask);
            float fR = 0.6F;
            int n = 12;
            int ma = (1 << n) - 1;
            x += ((iR & ma) / (float) (ma)) * fR - 0.5f * fR;
            z += (((iR >> n) & ma) / (float) (ma)) * fR - 0.5f * fR;
        }

        float h = 1f;
        float w = 1f;
        float sideOffset = 1 - w;
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
            attr.v[v].setFaceVertDir(0);
            attr.v[v].setNoDirection();
        }

        attr.v0.setUV(sideOffset, 0);
        attr.v0.setPos(x + sideOffset, y, z + sideOffset);

        attr.v1.setUV(sideOffset, h);
        attr.v1.setPos(x + sideOffset, y + h, z + sideOffset);

        attr.v2.setUV(1 - sideOffset, h);
        attr.v2.setPos(x + 1 - sideOffset, y + h, z + 1 - sideOffset);

        attr.v3.setUV(1 - sideOffset, 0);
        attr.v3.setPos(x + 1 - sideOffset, y, z + 1 - sideOffset);
        
        
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
        
        attr.put(this.vbuffer[targetBuffer]);
        
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        

        attr.put(this.vbuffer[targetBuffer]);

        attr.v0.setUV(sideOffset, 0);
        attr.v0.setPos(x + 1 - sideOffset, y, z + sideOffset);

        attr.v1.setUV(sideOffset, h);
        attr.v1.setPos(x + 1 - sideOffset, y + h, z + sideOffset);

        attr.v2.setUV(1 - sideOffset, h);
        attr.v2.setPos(x + sideOffset, y + h, z + 1 - sideOffset);

        attr.v3.setUV(1 - sideOffset, 0);
        attr.v3.setPos(x + sideOffset, y, z + 1 - sideOffset);

        attr.setReverse(false);
        {
            float nx=nside;
            float nz=nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        

        attr.put(this.vbuffer[targetBuffer]);
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=-nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }


        attr.put(this.vbuffer[targetBuffer]);
        return 4;
    }

}
