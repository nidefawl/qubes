/**
 * 
 */
package nidefawl.qubes.models.voxel;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.assets.AssetVoxModel;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelVox {

    public VertexBuffer buf = new VertexBuffer(1024*64);
    public VertexBuffer shadowBuf = new VertexBuffer(1024*16);
    public GLTriBuffer gpuBuf = null;
    public GLTriBuffer gpuShadowBuf = null;
    public AssetVoxModel asset;
    public ModelVoxPalette palette;
    public BlockPos size;
    public byte[] volume;
    public boolean needsDraw = true;
    public long reRender=0;
    
    /**
     * 
     */
    public ModelVox() {
    }
    public ModelVox(AssetVoxModel asset) {
        this.asset = asset;
        this.size = asset.size;
        this.palette = asset.palette;
        this.volume = new byte[this.size.getVolume()];
        for (int idx = 0; idx < this.asset.voxels.length; idx++) {
            int v = this.asset.voxels[idx];
            int c = v & 0xFF;
            v >>= 8;
            int y = v & 0xFF;
            v >>= 8;
            int z = v & 0xFF;
            v >>= 8;
            int x = v & 0xFF;
            int idx1 = y * this.size.z * this.size.x + z * this.size.x + x;

            if (idx1 < 0 || idx1 >= this.volume.length) {
                System.err.println("OUT OF BOUNDS VOXEL POS "+idx1+", (arr len: "+volume.length+")");
                continue;
            }
            if (c < 0 || c >= this.palette.table.length) {
                System.err.println("OUT OF BOUNDS VOXEL COLOR "+c+", (palette len: "+palette.table.length+")");
                continue;
            }
            this.volume[idx1] = (byte) c;
        }
    }

    public void render(int pass) {
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>42200) {
            this.reRender = System.currentTimeMillis();

            this.needsDraw = false;
            this.buf.reset();
            this.shadowBuf.reset();
            BlockFaceAttr attr = new BlockFaceAttr();
            attr.setUseGlobalRenderOffset(false);
            for (int x = 0; x < this.size.x; x++) {
                for (int z = 0; z < this.size.z; z++) {
                    for (int y = 0; y < this.size.y; y++) {
                        int v = get(x, y, z);
                        if (v != 0) {
                            int c = this.palette.table[v-1];
//                            System.out.println(Integer.toHew xString(c));
                            for (int i = 0; i < 4; i++) {
                                attr.v[i].setColorRGB(c);
                                attr.v[i].setNoDirection();
                            }
                            attr.setType(1);
                            attr.setPass(0);
//                            attr.setTex(Block.grass.getTexture(Dir.DIR_POS_Y, 0));
                            attr.v0.setUV(0, 0);
                            attr.v1.setUV(1, 0);
                            attr.v2.setUV(1, 1);
                            attr.v3.setUV(0, 1);
                            if (!isOccluding(x, y-1, z)) {
                                attr.setReverse(false);
                                attr.setFaceDir(Dir.DIR_NEG_Y);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_NEG_Y);
                                }
                                attr.setAO(calcNegY(x, y, z));
                                attr.setNormal(0, -1, 0);
                                attr.v0.setPos(x, y, z);
                                attr.v1.setPos(x + 1, y, z);
                                attr.v2.setPos(x + 1, y, z + 1);
                                attr.v3.setPos(x, y, z + 1);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                            if (!isOccluding(x, y+1, z)) {
                                attr.setReverse(true);
                                attr.setFaceDir(Dir.DIR_POS_Y);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_POS_Y);
                                }
                                attr.setAO(calcPosY(x, y, z));
                                attr.setNormal(0, 1, 0);
                                attr.v0.setPos(x, y + 1, z);
                                attr.v1.setPos(x + 1, y + 1, z);
                                attr.v2.setPos(x + 1, y + 1, z + 1);
                                attr.v3.setPos(x, y + 1, z + 1);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                            if (!isOccluding(x+1, y, z)) {
                                attr.setReverse(false);
                                attr.setFaceDir(Dir.DIR_POS_X);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_POS_X);
                                }
                                attr.setAO(calcPosX(x, y, z));
                                attr.setNormal(1, 0, 0);
                                attr.v0.setPos(x + 1, y, z + 1);
                                attr.v1.setPos(x + 1, y, z);
                                attr.v2.setPos(x + 1, y + 1, z);
                                attr.v3.setPos(x + 1, y + 1, z + 1);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                            if (!isOccluding(x-1, y, z)) {
                                attr.setFaceDir(Dir.DIR_NEG_X);
                                attr.v0.setUV(1, 0);
                                attr.v1.setUV(0, 0);
                                attr.v2.setUV(0, 1);
                                attr.v3.setUV(1, 1);
                                attr.setReverse(true);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_NEG_X);
                                }
                                attr.setAO(calcNegX(x, y, z));
                                attr.setNormal(-1, 0, 0);
                                attr.v0.setPos(x, y, z + 1);
                                attr.v1.setPos(x, y, z);
                                attr.v2.setPos(x, y + 1, z);
                                attr.v3.setPos(x, y + 1, z + 1);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                            if (!isOccluding(x, y, z+1)) {
                                attr.setFaceDir(Dir.DIR_POS_Z);
                                attr.v0.setUV(0, 0);
                                attr.v1.setUV(1, 0);
                                attr.v2.setUV(1, 1);
                                attr.v3.setUV(0, 1);
                                attr.setReverse(false);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_POS_Z);
                                }
                                attr.setAO(calcPosZ(x, y, z));
                                attr.setNormal(0, 0, 1);
                                attr.v0.setPos(x, y, z + 1);
                                attr.v1.setPos(x + 1, y, z + 1);
                                attr.v2.setPos(x + 1, y + 1, z + 1);
                                attr.v3.setPos(x, y + 1, z + 1);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                            if (!isOccluding(x, y, z-1)) {
                                attr.setFaceDir(Dir.DIR_NEG_Z);
                                attr.v0.setUV(1, 0);
                                attr.v1.setUV(0, 0);
                                attr.v2.setUV(0, 1);
                                attr.v3.setUV(1, 1);
                                attr.setReverse(true);
                                for (int i = 0; i < 4; i++) {
                                    attr.v[i].setFaceVertDir(Dir.DIR_NEG_Z);
                                }
                                attr.setAO(calcNegZ(x, y, z));
                                attr.setNormal(0, 0, -1);
                                attr.v0.setPos(x, y, z);
                                attr.v1.setPos(x + 1, y, z);
                                attr.v2.setPos(x + 1, y + 1, z);
                                attr.v3.setPos(x, y + 1, z);
                                attr.put(buf);
                                attr.putBasic(shadowBuf);
                            }
                        }
                    }
                }
            }
            if (this.gpuShadowBuf == null) {
                this.gpuShadowBuf = new GLTriBuffer(GL15.GL_STATIC_DRAW);
            }
            this.shadowBuf.makeTriIdx();
            this.gpuShadowBuf.upload(shadowBuf);
            if (this.gpuBuf == null) {
                this.gpuBuf = new GLTriBuffer(GL15.GL_STATIC_DRAW);
            }
            this.buf.makeTriIdx();
            this.gpuBuf.upload(buf);
        }
        if (pass == WorldRenderer.PASS_SHADOW_SOLID) {
            this.gpuShadowBuf.draw();
        } else {
            this.gpuBuf.draw();
        }
    }
    public boolean isOccluding(int x, int y, int z) {
        return get(x, y, z) != 0;
    }
    /**
     * @param x
     * @param y
     * @param z
     * @return
     */
    public int get(int x, int y, int z) {
        if (x < 0 || x >= this.size.x) return 0;
        if (y < 0 || y >= this.size.y) return 0;
        if (z < 0 || z >= this.size.z) return 0;
        int idx1 = y * this.size.z * this.size.x + z * this.size.x + x;
        int v = volume[idx1] & 0xFF;
        return v;
    }
    public void release() {
        if (this.gpuBuf != null) {
            this.gpuBuf.release();
            this.gpuBuf = null;
            this.gpuShadowBuf.release();
            this.gpuShadowBuf = null;
            this.buf = null;
            this.shadowBuf = null;
        }
    }

    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3&0x3)<<6)|((ao2&0x3)<<4)|((ao1&0x3)<<2)|(ao0&0x3);
    }

    int vertexAO(boolean side1, boolean side2, boolean corner) {
        int ao = 3;
        if (!(side1)) 
            ao--;
        if (!(side2)) 
            ao--;
        if (!(corner)) 
            ao--;
        return ao;
    }

    private int calcPosZ(int x, int y, int z) {
        z++;
        if (isOccluding(x, y, z)) {
            return 0;
        }
        boolean pp = isOccluding(x+1, y+1, z);
        boolean np = isOccluding(x-1, y+1, z);
        boolean pn = isOccluding(x+1, y-1, z);
        boolean nn = isOccluding(x-1, y-1, z);
        boolean cn = isOccluding(x, y-1, z);
        boolean nc = isOccluding(x-1, y, z);
        boolean cp = isOccluding(x, y+1, z);
        boolean pc = isOccluding(x+1, y, z);
        int ao0 = vertexAO(cn, nc, nn);
        int ao1 = vertexAO(cn, pc, pn);
        int ao2 = vertexAO(cp, pc, pp);
        int ao3 = vertexAO(cp, nc, np);
        return maskAO(ao0, ao1, ao2, ao3);
    }
    
    private int calcNegZ(int x, int y, int z) {
        z--;
        if (isOccluding(x, y, z)) {
            return 0;
        }

        
        boolean pp = isOccluding(x + 1, y + 1, z);
        boolean np = isOccluding(x - 1, y + 1, z);
        boolean pn = isOccluding(x + 1, y - 1, z);
        boolean nn = isOccluding(x - 1, y - 1, z);
        boolean cn = isOccluding(x, y - 1, z);
        boolean nc = isOccluding(x - 1, y, z);
        boolean cp = isOccluding(x, y + 1, z);
        boolean pc = isOccluding(x + 1, y, z);
        int ao0 = vertexAO(cn, pc, pn);
        int ao1 = vertexAO(cn, nc, nn);
        int ao2 = vertexAO(cp, nc, np);
        int ao3 = vertexAO(cp, pc, pp);
        return maskAO(ao0, ao1, ao2, ao3);
    }

    private int calcPosX(int x, int y, int z) {
        x++;
        if (isOccluding(x, y, z)) {
            return 0;
        }
        boolean pp = isOccluding(x, y+1, z+1);
        boolean pn = isOccluding(x, y+1, z-1);
        boolean np = isOccluding(x, y-1, z+1);
        boolean nn = isOccluding(x, y-1, z-1);
        boolean nc = isOccluding(x, y-1, z);
        boolean cn = isOccluding(x, y, z-1);
        boolean pc = isOccluding(x, y+1, z);
        boolean cp = isOccluding(x, y, z+1);
        int ao1 = vertexAO(cn, nc, nn);//bottom right
        int ao2 = vertexAO(cn, pc, pn); //top right
        int ao3 = vertexAO(cp, pc, pp); //top left
        int ao0 = vertexAO(cp, nc, np); //bottom left
        return maskAO(ao0, ao1, ao2, ao3);
    }
    private int calcNegX(int x, int y, int z) {
        x--;
        if (isOccluding(x, y, z)) {
            return 0;
        }
        boolean pp = isOccluding(x, y+1, z+1);
        boolean pn = isOccluding(x, y+1, z-1);
        boolean np = isOccluding(x, y-1, z+1);
        boolean nn = isOccluding(x, y-1, z-1);
        boolean nc = isOccluding(x, y-1, z);
        boolean cn = isOccluding(x, y, z-1);
        boolean pc = isOccluding(x, y+1, z);
        boolean cp = isOccluding(x, y, z+1);
        int ao3 = vertexAO(cn, pc, pn);//top left
        int ao0 = vertexAO(cn, nc, nn);//bottom left
        int ao1 = vertexAO(cp, nc, np); //bottom right
        int ao2 = vertexAO(cp, pc, pp); //top right
        return maskAO(ao0, ao1, ao2, ao3);
    }

    private int calcPosY(int x, int y, int z) {
        y++;
        if (isOccluding(x, y, z)) {
            return 0;
        }
        boolean pp = isOccluding(x+1, y, z+1);
        boolean pn = isOccluding(x+1, y, z-1);
        boolean np = isOccluding(x-1, y, z+1);
        boolean nn = isOccluding(x-1, y, z-1);
        boolean nc = isOccluding(x-1, y, z);
        boolean cn = isOccluding(x, y, z-1);
        boolean pc = isOccluding(x+1, y, z);
        boolean cp = isOccluding(x, y, z+1);
        int ao0 = vertexAO(cn, nc, nn);
        int ao3 = vertexAO(cp, nc, np);
        int ao2 = vertexAO(cp, pc, pp);
        int ao1 = vertexAO(cn, pc, pn);
        return maskAO(ao0, ao1, ao2, ao3);
    }
    private int calcNegY(int x, int y, int z) {
        y--;
        if (isOccluding(x, y, z)) {
            return 0;
        }
        boolean pp = isOccluding(x+1, y, z+1);
        boolean pn = isOccluding(x+1, y, z-1);
        boolean np = isOccluding(x-1, y, z+1);
        boolean nn = isOccluding(x-1, y, z-1);
        boolean nc = isOccluding(x-1, y, z);
        boolean cn = isOccluding(x, y, z-1);
        boolean pc = isOccluding(x+1, y, z);
        boolean cp = isOccluding(x, y, z+1);
        int ao0 = vertexAO(cn, nc, nn);
        int ao3 = vertexAO(cp, nc, np);
        int ao2 = vertexAO(cp, pc, pp);
        int ao1 = vertexAO(cn, pc, pn);
        return maskAO(ao0, ao1, ao2, ao3);
    }


}
