package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static nidefawl.qubes.render.WorldRenderer.PASS_LOD;
import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static nidefawl.qubes.render.region.RegionRenderer.REGION_SIZE_BLOCKS_MASK;
import static nidefawl.qubes.render.region.RegionRenderer.REGION_SIZE_BLOCK_SIZE_BITS;
import static nidefawl.qubes.render.region.RegionRenderer.SLICE_HEIGHT_BLOCK_MASK;

import java.util.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class MeshUpdateTask {
    public final Mesher     mesher = new Mesher();
    public final BlockRenderer     blockRenderer = new BlockRenderer();
    public final ChunkRenderCache ccache = new ChunkRenderCache();
//    final Tess[] tess  = new Tess[NUM_PASSES];
    final BlockFaceAttr attr = new BlockFaceAttrUINT();
//    final ByteBuffer directBuffer = BufferUtils.createAlignedByteBuffer(1024*1024*10, 16);

    public int              worldInstance;
    private boolean         meshed;
    private MeshedRegion mr;
    final VertexBuffer[] vbuffer = new VertexBuffer[NUM_PASSES];
    
    private int shadowDrawMode;
    final static int VIS_LEN = RegionRenderer.REGION_SIZE_BLOCKS;
    final static int VIS_BITS = VIS_LEN*VIS_LEN*VIS_LEN;
    final static int[] EDGES = calcEdges();
    public static class VisibilityCache {
        public final BitSet bits = new BitSet(6*6);

        public void setAll(boolean b) {
            bits.set(0, 36, b);
        }
        public void set(int idx, boolean b) {
            bits.set(idx, b);
        }
        public void setVisible(int edgeMask) {
            for (int i = 0; i < 6; i++) {
                if ((edgeMask & (1<<i)) != 0) {
                    for (int j = 0; j < 6; j++) {
                        if ((edgeMask & (1<<j)) != 0) {
                            bits.set(j*6+i, true);
                            bits.set(i*6+j, true);
                        }
                    }
                }
            }
        }
        public boolean isVisible(int d1, int d2) {
            return this.bits.get(d1*6+d2);
        }
    }
    static int idx(int x, int y, int z)
    {
        return y << 10 | z << 5 | x;
    }
    static class RegionFaceOpacityCache {
        public final BitSet bits = new BitSet(VIS_BITS);
        public int empty = 0;


        public void setOpaque(int x, int y, int z) {
            bits.set(idx(x, y, z), true);
            empty--;
        }
        public void reset() {
            bits.set(0, VIS_BITS, false);
            empty = VIS_BITS;
        }
        public VisibilityCache fill() {
            VisibilityCache cache = new VisibilityCache();

            if (this.empty <= 0) {
                cache.setAll(false);
                return cache;
            }
//            System.out.println(this.empty);
//            System.out.println(EDGES);
            Queue<Integer> queue = new ArrayDeque<>();
            for (int i = 0; i < EDGES.length; i++) {
                int idx = EDGES[i];
                if (!bits.get(idx)) {
                    int edgeMask = 0;
                    queue.clear();
                    queue.add(idx);
                    bits.set(idx, true);
                    while (!queue.isEmpty()) {
                        int qidx = queue.poll().intValue();
                        edgeMask |= getEdge(qidx);
                        if (edgeMask == 0x3F) {
                            break;
                        }
                        for (int j = 0; j < 6; j++) {
                            int nextIdx = getNeighbour(j, qidx);
                            if (nextIdx >= 0 && !bits.get(nextIdx)) {
                                bits.set(nextIdx, true);
                                queue.add(nextIdx);
                            }
                        }
                    }
                    if (edgeMask == 0x3F) {
                        cache.setAll(true);
                        return cache;
                    }
                    cache.setVisible(edgeMask);
                }
            }
            return cache;
        }
        private int getNeighbour(int j, int qidx) {
            switch (j) {
                case Dir.DIR_POS_X:
                    if ((qidx & 31) == 31) {
                        return -1;
                    }
                    return qidx + 1;
                case Dir.DIR_NEG_X:
                    if ((qidx & 31) == 0) {
                        return -1;
                    }
                    return qidx - 1;
                case Dir.DIR_POS_Y:
                    if (((qidx>>10) & 31) == 31) {
                        return -1;
                    }
                    return qidx + (1 << 10);
                case Dir.DIR_NEG_Y:
                    if (((qidx>>10) & 31) == 0) {
                        return -1;
                    }
                    return qidx - (1 << 10);
                case Dir.DIR_POS_Z:
                    if (((qidx>>5) & 31) == 31) {
                        return -1;
                    }
                    return qidx + (1 << 5);
                case Dir.DIR_NEG_Z:
                    if (((qidx>>5) & 31) == 0) {
                        return -1;
                    }
                    return qidx - (1 << 5);
            }
            return -1;
        }
        private int getEdge(int qidx) {
            int edgeMask = 0;
            int xEdge = (qidx & 31);
            qidx >>= 5;
            int zEdge = (qidx & 31);
            qidx >>= 5;
            int yEdge = (qidx & 31);
            if (xEdge == 0) {
                edgeMask |= 2;
            } else if (xEdge == 31) {
                edgeMask |= 1;
            }
            if (yEdge == 0) {
                edgeMask |= 8;
            } else if (yEdge == 31) {
                edgeMask |= 4;
            }
            if (zEdge == 0) {
                edgeMask |= 32;
            } else if (zEdge == 31) {
                edgeMask |= 16;
            }
            return edgeMask;
        }
    }
    RegionFaceOpacityCache vis = new RegionFaceOpacityCache();
    private VisibilityCache visCache;
    public MeshUpdateTask() {
        for (int i = 0; i < this.vbuffer.length; i++) {
            this.vbuffer[i] = new VertexBuffer(1024*1024*2);
        }
        this.attr.setUseGlobalRenderOffset(true);
    }

    private static int[] calcEdges() {
        ArrayList<Integer> list = new ArrayList<>();
        for (int x = 0; x < VIS_LEN; x++) {
            for (int z = 0; z < VIS_LEN; z++) {
                for (int y = 0; y < VIS_LEN; y++) {
                    if (x == 0 || y == 0 || z == 0 || x == VIS_LEN-1 || y == VIS_LEN-1 || z == VIS_LEN-1) {
                        list.add(idx(x, y, z));
                    }
                }
            }
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public boolean prepare(IBlockWorld world, ChunkManager mgr, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        this.ccache.flush();
        if (this.ccache.cache(world, mgr, mr, renderChunkX, renderChunkZ)) {
            this.visCache = null;
            this.mr = mr;
            this.shadowDrawMode = GameBase.getSettings().renderSettings.shadowDrawMode;
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

        this.mr.visCache = this.visCache;
        if (this.meshed) {
            if (GPUProfiler.PROFILING_ENABLED) {
                GPUProfiler.start("upload");
            }
            long l = System.nanoTime();
            this.mr.preUploadBuffers();

            for (int i = 0; i < NUM_PASSES; i++) {
                this.mr.uploadBuffer(i, this.vbuffer[i], this.shadowDrawMode);
            }
//            this.mr.compileDisplayList(this.tess);
            Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
            if (GPUProfiler.PROFILING_ENABLED) {
                GPUProfiler.end();
            }
            int b = 0;
            for (int i = 0; i < NUM_PASSES; i++) {
                b+=vbuffer[i].get().length*4+vbuffer[i].getTriIdxBuffer().length*4;
            }
            if (Stats.regionUpdates%40==0) {
                float mb = b/(float)(1024*1024);
//                System.out.printf("%d %d %.2fMb\n",Stats.regionUpdates,Mesher.avgUsage, mb);   
            }
            
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

        IBlockWorld w = this.ccache.getWorld();
        if (w != null) {
//            System.out.println(Thread.currentThread().getName()+" on "+this.mr);
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
//                        if (Block.leaves.getBlocks().contains(Block.get(face.bs.type))) {
//                            continue;
//                        }
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
                vis.reset();
                for (int x = 0; x < VIS_LEN; x++) {
                    for (int z = 0; z < VIS_LEN; z++) {
                        for (int y = 0; y < VIS_LEN; y++) {
                            int type = this.ccache.getType(x, y+yOff, z);
                            if (type!=0) {
                                vis.setOpaque(x, y, z);
                            }
                            
                        }
                    }
                }
                this.visCache = vis.fill();
                Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
                this.meshed = true;
//                System.out.println(Thread.currentThread().getName()+" done "+this.mr);
//                for (int i = 0; i < NUM_PASSES; i++) {
//                    System.out.println(this.mr+" num vertices PASS["+i+"] = "+(mr.getNumVertices(i)));
//                }
                return true;
            } catch (Exception e) {
                GameBase.baseInstance.setException(new GameError("Error while updating region", e));
            }
            return false;
        }
        return false;
    }


    public MeshedRegion getRegion() {
        return this.mr;
    }

    public void destroy() {
    }

}
