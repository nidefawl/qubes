package nidefawl.qubes.meshing;

import static nidefawl.qubes.meshing.BlockFaceAttr.BLOCK_FACE_INT_SIZE;

import static nidefawl.qubes.meshing.BlockFaceAttr.PASS_2_BLOCK_FACE_INT_SIZE;
import static nidefawl.qubes.meshing.BlockFaceAttr.PASS_3_BLOCK_FACE_INT_SIZE;
import static nidefawl.qubes.render.region.RegionRenderer.*;

import static nidefawl.qubes.render.WorldRenderer.*;

import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
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
    final int[][] buffers = new int[NUM_PASSES][];
    final int[] vertexCount = new int[NUM_PASSES];
    final int[] bufferIdx = new int[NUM_PASSES];
    private int shadowDrawMode;
    
    public MeshUpdateTask() {
//        for (int i = 0; i < this.tess.length; i++) {
//            this.tess[i] = new Tess(true);
//        }
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
                if (this.buffers[i] == null) 
                    this.buffers[i] = new int[0];
                this.mr.uploadBuffer(i, this.buffers[i], this.bufferIdx[i], this.vertexCount[i], this.shadowDrawMode);
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
              int xOff = this.mr.rX << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS);
              int yOff = this.mr.rY << (RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
              int zOff = this.mr.rZ << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS);
                long l = System.nanoTime();
                this.mesher.mesh(w, this.ccache, this.mr.rY);
                Stats.timeMeshing += (System.nanoTime()-l) / 1000000.0D;
                l = System.nanoTime();
                int FACE_SHADOW_INT_SIZE = PASS_2_BLOCK_FACE_INT_SIZE;
                if (shadowDrawMode > 0) {
                    FACE_SHADOW_INT_SIZE = PASS_3_BLOCK_FACE_INT_SIZE;
                }
                for (int i = 0; i < PASS_LOD && this.mr.isValid; i++) {
                    this.vertexCount[i] = 0;
                    this.bufferIdx[i] = 0;
                    List<BlockFace> mesh = this.mesher.getMeshes(i);
                    
                    int numMeshedFaces = mesh.size();
                    int FACE_INT_SIZE = BLOCK_FACE_INT_SIZE;
                    if (i == PASS_SHADOW_SOLID) { 
                        FACE_INT_SIZE = FACE_SHADOW_INT_SIZE;
                    }
                    int extraBufferLen = (int)(numMeshedFaces*3.3);
                    checkBufferSize(i, extraBufferLen * FACE_INT_SIZE);
                    int numFaces = 0;
                    int[] buffer = this.buffers[i];
                    attr.setOffset(xOff, yOff, zOff);
                    for (int m = 0; m < numMeshedFaces; m++) {
                        BlockFace face = mesh.get(m);
                        if (i == PASS_SHADOW_SOLID) {
                            if (shadowDrawMode == 0)
                                numFaces += face.drawBasic(attr, buffer, numFaces*FACE_INT_SIZE);
                            else
                                numFaces += face.drawShadowTextured(attr, buffer, numFaces*FACE_INT_SIZE);
                        } else {
//                            int fdir = face.bs.axis<<1|face.bs.face;
//                            if (fdir==Dir.DIR_POS_X||fdir==Dir.DIR_POS_Z)
//                                continue;
                            numFaces += face.draw(attr, buffer, numFaces*FACE_INT_SIZE);
                        }
                        if (numFaces >= extraBufferLen) {
                            extraBufferLen = (int)(extraBufferLen*2.3);
                            reallocBuffer(i, extraBufferLen * FACE_INT_SIZE);
//                            throw new RuntimeException("EXCEEDING BUFFER SIZE, IMPL REALLOC WITH ARRAY COPY");
                            
                        }
                    }
                    
                   
                    this.bufferIdx[i] = numFaces*FACE_INT_SIZE;
                    this.vertexCount[i] = numFaces*4;
                }
                this.vertexCount[PASS_LOD] = 0;
                this.bufferIdx[PASS_LOD] = 0;
                int n = this.mesher.getRenderType1Blocks();
                if (n > 0) {
                    int approxFaces = (int)(n*10);
                    int FACE_INT_SIZE = BLOCK_FACE_INT_SIZE;
                    int lodBufferSize = checkBufferSize(PASS_LOD, approxFaces * FACE_INT_SIZE);
                    int shadowBufferSize = this.buffers[PASS_SHADOW_SOLID].length;
                    int shadowBufferIndex = this.bufferIdx[PASS_SHADOW_SOLID];
                    int requiredShadowBufferSize = shadowBufferIndex + approxFaces * FACE_SHADOW_INT_SIZE;
                    reallocBuffer(PASS_SHADOW_SOLID, requiredShadowBufferSize);
                    int numFaces = 0;
                    attr.setOffset(0, 0, 0);
                    blockRenderer.setShadowBuffer(this.buffers[PASS_SHADOW_SOLID], shadowBufferIndex, this.shadowDrawMode, FACE_SHADOW_INT_SIZE);
                    blockRenderer.preRender(w, this.buffers[PASS_LOD], 0, FACE_INT_SIZE, this.ccache, attr);
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
                        int faces = blockRenderer.render(PASS_LOD, x, y, z);
                        numFaces += faces;
                        if (numFaces*FACE_INT_SIZE >= lodBufferSize) {
                            throw new GameError("BUFFER INDEX OUT OF BOUNDS, some block is drawing too many faces. implement logic to handle reallocation of buffer");
                        }   
                    }
                   
                    this.bufferIdx[PASS_LOD] = numFaces*FACE_INT_SIZE;
                    this.vertexCount[PASS_LOD] = numFaces*4;
                    this.bufferIdx[PASS_SHADOW_SOLID] += this.blockRenderer.numShadowFaces*FACE_SHADOW_INT_SIZE;
                    this.vertexCount[PASS_SHADOW_SOLID] += this.blockRenderer.numShadowFaces*4;
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

    private int checkBufferSize(int bufferIdx, int length) {
        if (this.buffers[bufferIdx] == null || this.buffers[bufferIdx].length < length) {
            int newSize = (length+2048);
//            System.out.println("realloc buffer to length "+newSize);
            this.buffers[bufferIdx] = new int[newSize];
        }
        return this.buffers[bufferIdx].length;
    }
    private int reallocBuffer(int bufferIdx, int length) {
        if (this.buffers[bufferIdx] == null || this.buffers[bufferIdx].length < length) {
            int newSize = (length+2048);
            System.out.println("realloc buffer to length "+newSize);
            int newBuffer[] = new int[newSize];
            System.arraycopy(this.buffers[bufferIdx], 0, newBuffer, 0, this.buffers[bufferIdx].length);
            this.buffers[bufferIdx] = newBuffer;
        }
        return this.buffers[bufferIdx].length;
    }

    public MeshedRegion getRegion() {
        return this.mr;
    }

}
