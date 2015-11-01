package nidefawl.qubes.meshing;

import static nidefawl.qubes.meshing.BlockFaceAttr.BLOCK_FACE_INT_SIZE;

import static nidefawl.qubes.meshing.BlockFaceAttr.PASS_2_BLOCK_FACE_INT_SIZE;
import static nidefawl.qubes.meshing.BlockFaceAttr.PASS_3_BLOCK_FACE_INT_SIZE;
import static nidefawl.qubes.render.region.RegionRenderer.*;

import static nidefawl.qubes.render.WorldRenderer.*;

import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class MeshUpdateTask {
    public final Mesher     mesher = new Mesher();
    public final BlockRenderer     blockRenderer = new BlockRenderer();
    public final ChunkRenderCache ccache = new ChunkRenderCache();
//    final Tess[] tess  = new Tess[NUM_PASSES];
    final BlockFaceAttr attr = new BlockFaceAttr();
//    final ByteBuffer directBuffer = BufferUtils.createAlignedByteBuffer(1024*1024*10, 16);

    public int              worldInstance;
    private boolean         meshed;
    private MeshedRegion mr;
    final VertexBuffer[] vbuffer = new VertexBuffer[NUM_PASSES];
    
    private int shadowDrawMode;
    
    public MeshUpdateTask() {
        for (int i = 0; i < this.vbuffer.length; i++) {
            this.vbuffer[i] = new VertexBuffer(1024*1024*2);
        }
        this.attr.setUseGlobalRenderOffset(true);
    }

    public boolean prepare(WorldClient world, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        this.ccache.flush();
        if (this.ccache.cache(world, mr, renderChunkX, renderChunkZ)) {
            this.mr = mr;
            this.shadowDrawMode = Game.instance.settings.shadowDrawMode;
            return true;
        } else {
//            System.out.println("cannot render "+mr.rX+"/"+mr.rZ);
        }
        return false;
    }

    public boolean finish(int id) {
        if (!isValid(id)) {
//            mr.renderState = RegionRenderer.RENDER_STATE_INIT;
            this.ccache.flush();
            return true;
        }
        if (this.meshed) {
            long l = System.nanoTime();
            this.mr.preUploadBuffers();

            for (int i = 0; i < NUM_PASSES; i++) {
                this.mr.uploadBuffer(i, this.vbuffer[i], this.shadowDrawMode);
            }
//            this.mr.compileDisplayList(this.tess);
            Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
        } else {
            //TODO: flush display list if compile failed, or ignore
        }
        this.mr.xNeg = this.ccache.getWest() != null;
        this.mr.xPos = this.ccache.getEast() != null;
        this.mr.zNeg = this.ccache.getNorth() != null;
        this.mr.zPos = this.ccache.getSouth() != null;
        this.mr.xNeg = false;
        this.mr.zNeg = false;
        this.mr.zPos = false;
        this.mr.xPos = false;
        this.mr.isRenderable = true;
        this.ccache.flush();
        return true;
    }

    public boolean isValid(int id) {
        return this.worldInstance == id && this.mr.isValid;
    }

    public boolean updateFromThread() {
        Stats.regionUpdates++;
//        if (this.mr.isEmpty()) {
//            return true;
//        }
        World w = Game.instance.getWorld();
        
        if (w != null) {
            try {
              int xOff = (this.mr.rX << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS));
              int yOff = this.mr.rY << (RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
              int zOff = (this.mr.rZ << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS));
                long l = System.nanoTime();
                this.mesher.mesh(this.ccache, this.mr.rY);
                Stats.timeMeshing += (System.nanoTime()-l) / 1000000.0D;
                l = System.nanoTime();
                for (int a = 0; a < this.vbuffer.length; a++) {
                    vbuffer[a].reset();
                }
                for (int i = 0; i < PASS_LOD && this.mr.isValid; i++) {
                    List<BlockFace> mesh = this.mesher.getMeshes(i);
                    int numMeshedFaces = mesh.size();
                    attr.setOffset(xOff, yOff, zOff);
                    for (int m = 0; m < numMeshedFaces; m++) {
                        BlockFace face = mesh.get(m);
                        if (i == PASS_SHADOW_SOLID) {
                            if (shadowDrawMode == 0)
                                face.drawBasic(attr, vbuffer[i]);
                            else
                                face.drawShadowTextured(attr, vbuffer[i]);
                        } else {
                            face.draw(attr, vbuffer[i]);
                        }
                    }
                }
                int n = this.mesher.getRenderType1Blocks();
                if (n > 0) {
                    attr.setOffset(0, 0, 0);
                    blockRenderer.setBuffers(this.vbuffer, this.shadowDrawMode);
                    blockRenderer.preRender(w, this.ccache, attr);
                    for (int j = 0; j < n; j++) {
                        short c = this.mesher.getBlockPos(j);
                        int z = (c) & REGION_SIZE_BLOCKS_MASK;
                        c >>= REGION_SIZE_BLOCK_SIZE_BITS;
                        int x = (c) & REGION_SIZE_BLOCKS_MASK;
                        c >>= REGION_SIZE_BLOCK_SIZE_BITS;
                        int y = (c&SLICE_HEIGHT_BLOCK_MASK);
                        x += xOff;
                        y += yOff;
                        z += zOff;
                        blockRenderer.render(x, y, z);
                    }
                }
            
                Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
                this.meshed = true;
                return true;
            } catch (Exception e) {
                Game.instance.setException(new GameError("Error while updating region", e));
            }
            return false;
        }
        return false;
    }


    public MeshedRegion getRegion() {
        return this.mr;
    }

}
