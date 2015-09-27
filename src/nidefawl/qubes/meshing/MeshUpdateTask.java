package nidefawl.qubes.meshing;

import static nidefawl.qubes.meshing.BlockFaceAttr.BLOCK_FACE_INT_SIZE;

import static nidefawl.qubes.meshing.BlockFaceAttr.PASS_2_BLOCK_FACE_INT_SIZE;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;

import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class MeshUpdateTask {
    public final Mesher     mesher = new Mesher();
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
    
    public MeshUpdateTask() {
//        for (int i = 0; i < this.tess.length; i++) {
//            this.tess[i] = new Tess(true);
//        }
    }

    public boolean prepare(WorldClient world, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        this.ccache.flush();
        if (this.ccache.cache(world, mr, renderChunkX, renderChunkZ)) {
            this.mr = mr;
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
                this.mr.uploadBuffer(i, this.buffers[i], this.bufferIdx[i], this.vertexCount[i]);
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
                for (int i = 0; i < NUM_PASSES && this.mr.isValid; i++) {
                    this.vertexCount[i] = 0;
                    this.bufferIdx[i] = 0;
//                    Tess tess = this.tess[i];
                    List<BlockFace> mesh = this.mesher.getMeshes(i);
//                    tess.resetState();
//                    if (i != 2) {
//                        tess.setColor(-1, 255);
//                        tess.setBrightness(0xf00000);
//                    }
                    attr.setOffset(xOff, yOff, zOff);
                    int numMeshedFaces = mesh.size();
                    final int FACE_INT_SIZE = i == 2 ? PASS_2_BLOCK_FACE_INT_SIZE : BLOCK_FACE_INT_SIZE;
                    final int extraBufferLen = (int)(numMeshedFaces*3.3);
                    int len = checkBufferSize(i, extraBufferLen * FACE_INT_SIZE);
                    int numFaces = 0;
                    int[] buffer = this.buffers[i];
                    for (int m = 0; m < numMeshedFaces; m++) {
                        BlockFace face = mesh.get(m);
                        if (i == 2) {
                            numFaces += face.drawBasic(attr, buffer, numFaces*FACE_INT_SIZE);
                        } else {
                            numFaces += face.draw(attr, buffer, numFaces*FACE_INT_SIZE);
                        }
                        if (numFaces >= extraBufferLen) {
                            System.err.println("EXCEEDING BUFFER SIZE, IMPL REALLOC WITH ARRAY COPY");
                        }
                    }
                    this.bufferIdx[i] = numFaces*FACE_INT_SIZE;
                    this.vertexCount[i] = numFaces*4;
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

    public MeshedRegion getRegion() {
        return this.mr;
    }

}
