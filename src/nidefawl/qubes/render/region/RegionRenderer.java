package nidefawl.qubes.render.region;

import static org.lwjgl.opengl.GL11.*;
import static nidefawl.qubes.render.WorldRenderer.*;

import java.nio.IntBuffer;
import java.util.*;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.render.AbstractRenderer;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IThreadedWork;
import nidefawl.qubes.util.ThreadedWorker;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class RegionRenderer extends AbstractRenderer implements IThreadedWork {
    public static final int REGION_SIZE_BITS            = 1;
    public static final int REGION_SIZE                 = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK            = REGION_SIZE - 1;
    public static final int REGION_SIZE_BLOCK_SIZE_BITS = Chunk.SIZE_BITS + REGION_SIZE_BITS;
    public static final int REGION_SIZE_BLOCKS          = 1 << REGION_SIZE_BLOCK_SIZE_BITS;
    public static final int REGION_SIZE_BLOCKS_MASK     = REGION_SIZE_BLOCKS - 1;
    public static final int SLICE_HEIGHT_BLOCK_BITS     = 5;
    public static final int SLICE_HEIGHT_BLOCKS         = 1 << SLICE_HEIGHT_BLOCK_BITS;
    public static final int SLICE_HEIGHT_BLOCK_MASK     = SLICE_HEIGHT_BLOCKS - 1;
    
    public static int RENDER_DISTANCE = 12;
    public static int OFFS_OVER       = 2;
    public static int LENGTH_OVER     = (RENDER_DISTANCE + OFFS_OVER) * 2 + 1;
    public static int HEIGHT_SLICES   = World.MAX_WORLDHEIGHT >> RegionRenderer.SLICE_HEIGHT_BLOCK_BITS;

    

    final Comparator<MeshedRegion> compareUpdateRenderers = new Comparator<MeshedRegion>() {
        @Override
        public int compare(MeshedRegion o1, MeshedRegion o2) {
            return sortUpdateRenderersCompare(o1, o2);
        }
    };
    final Comparator<MeshedRegion> compareRenderers = new Comparator<MeshedRegion>() {
        @Override
        public int compare(MeshedRegion o1, MeshedRegion o2) {
            return sortRenderersCompare(o1, o2);
        }
    };


    public int                      rendered;
    public int                      occlCulled;
    public int                      renderChunkX;
    public int                      renderChunkY;
    public int                      renderChunkZ;
    private ArrayList<MeshedRegion> renderList      = new ArrayList<>();
    private ArrayList<MeshedRegion> shadowRenderList      = new ArrayList<>();
    
    private ArrayList<MeshedRegion> regionsToUpdate = new ArrayList<>();
    static class ThreadContext {
         ArrayList<MeshedRegion> renderList      = new ArrayList<>();
         ArrayList<MeshedRegion> shadowRenderList      = new ArrayList<>();
         ArrayList<MeshedRegion> regionsToUpdate = new ArrayList<>();
         int workedOn=0;
    }
    boolean                         needsSortingUpdateRenderers    = false;
    
    
    final static int MAX_OCCL_QUERIES = 8;
    final static int MIN_DIST_OCCL = 1;
    final static int MIN_STATE_OCCL = Frustum.FRUSTUM_INSIDE_FULLY;
    final static boolean ENABLE_OCCL = !GPUProfiler.PROFILING_ENABLED;
    private int[] occlQueries = new int[MAX_OCCL_QUERIES];
    private MeshedRegion[] occlQueriesRunning = new MeshedRegion[MAX_OCCL_QUERIES];
    int queriesRunning = 0;
    private Shader occlQueryShader;
    private boolean startup;
    float camX; float camY; float camZ;
    MeshedRegion[][] regions;
    public int numV;
    private ThreadedWorker worker;
    private ThreadContext[] threadCtx;
    int rChunkX, rChunkY, rChunkZ;
    public boolean threadedCulling = false;
    
    
    /*
     * typedef struct {
     *   GLuint   index;
     *   GLuint   reserved;
     *   GLuint64 address;
     *   GLuint64 length;
     * } BindlessPtrNV;
     * 
     * typedef struct {
     *   uint count;
     *   uint primCount;
     *   uint firstIndex;
     *   uint baseVertex;
     *   uint baseInstance;
     *   DrawElementsIndirectCommand cmd;
     *   GLuint                      reserved;
     *   BindlessPtrNV               indexBuffer;
     *   BindlessPtrNV               vertexBuffers[];
     * } DrawElementsIndirectBindlessCommandNV;
     */
    
    protected static ReallocIntBuffer[] buffers = new ReallocIntBuffer[NUM_PASSES*4];
    protected static ReallocIntBuffer[] idxShortBuffers = new ReallocIntBuffer[NUM_PASSES*4];
    protected static MultiDrawIndirectBuffer buffer = new MultiDrawIndirectBuffer();
    
    static {
        for (int i = 0; i < idxShortBuffers.length; i++) {
            idxShortBuffers[i] = new ReallocIntBuffer();
        }
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new ReallocIntBuffer();
        }
    }
    
//    static void reallocBuffer(int pass, int len) {
//        if (buffers[pass] == null || buffers[pass].capacity() < len) {
//            if (buffers[pass] != null) {
//                buffers[pass] = Memory.reallocByteBufferAligned(buffers[pass], 64, len);
//            } else {
//                buffers[pass] = Memory.createByteBufferAligned(64, len);
//            }
//            intbuffers[pass] = buffers[pass].asIntBuffer();
//        }
//    }

    public void init() {
        initShaders();
        setRenderDistance(Game.instance.settings.chunkLoadDistance>>(REGION_SIZE_BITS));
        IntBuffer intbuf = Engine.glGenBuffers(this.occlQueries.length);
        intbuf.get(this.occlQueries);

        worker = new ThreadedWorker(4);
        worker.init();
        threadCtx = new ThreadContext[4];
        for (int i = 0; i < 4; i++) {
            this.threadCtx[i] = new ThreadContext();
        }
    }
    
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_occlQueryShader = assetMgr.loadShader(this, "terrain/occlusion_query");
            popNewShaders();
            occlQueryShader = new_occlQueryShader;
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }
    
    private MeshedRegion[][] create() {
        MeshedRegion[][] regions = new MeshedRegion[LENGTH_OVER*LENGTH_OVER][];
        for (int i = 0; i < regions.length; i++) {
            int x = (i%LENGTH_OVER);
            int z = (i/LENGTH_OVER);
            MeshedRegion[] ySlices = regions[i] = new MeshedRegion[HEIGHT_SLICES];
            for (int y = 0; y < HEIGHT_SLICES; y++) {
                MeshedRegion m = new MeshedRegion();
                m.rX = (x)-(RENDER_DISTANCE+OFFS_OVER);
                m.rY = y;
                m.rZ = (z)-(RENDER_DISTANCE+OFFS_OVER);
                m.updateBB();
                ySlices[y] = m;
            }
        }
        return regions;
    }

    private void reposition(int rChunkX, int rChunkZ) {
        int diffX = rChunkX-this.renderChunkX;
        int diffZ = rChunkZ-this.renderChunkZ;
        
        MeshedRegion[][] newRegions = new MeshedRegion[LENGTH_OVER*LENGTH_OVER][];
        for (int i = 0; i < newRegions.length; i++) {
            int x = (i%LENGTH_OVER);
            int z = (i/LENGTH_OVER);
            int oldX = x+diffX;
            int oldZ = z+diffZ;

            if (oldX>=0&&oldX<LENGTH_OVER&&oldZ>=0&&oldZ<LENGTH_OVER) {
                int oldXZPos = oldZ*LENGTH_OVER+oldX;
                MeshedRegion[] m = this.regions[oldXZPos];
                this.regions[oldXZPos] = null;
                newRegions[i] = m;
            } else {
                MeshedRegion[] ySlice = newRegions[i] = new MeshedRegion[HEIGHT_SLICES];
                for (int y = 0; y < HEIGHT_SLICES; y++) {
                    MeshedRegion m = new MeshedRegion();
                    m.rX = (x)-(RENDER_DISTANCE+OFFS_OVER);
                    m.rY = y;
                    m.rZ = (z)-(RENDER_DISTANCE+OFFS_OVER);
                    m.rX += rChunkX;
                    m.rZ += rChunkZ;
                    m.updateBB();
                    ySlice[y] = m;
                }
            }
        }
        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] m = this.regions[i];
            if (m != null) {
                for (int y = 0; y < HEIGHT_SLICES; y++) {
                    m[y].release();
                }
            }
            
        }
        
        this.regions = newRegions;
        this.renderChunkX = rChunkX;
        this.renderChunkZ = rChunkZ;
        
    }

    public MeshedRegion getByChunkCoord(int x, int y, int z) {
        if (y < 0) {
            y = renderChunkY;
        }
        x>>=REGION_SIZE_BITS;
        z>>=REGION_SIZE_BITS;
        x -= this.renderChunkX-RENDER_DISTANCE;
        z -= this.renderChunkZ-RENDER_DISTANCE;
        x+=OFFS_OVER;
        z+=OFFS_OVER;
        if (x>=0&&x<LENGTH_OVER&&z>=0&&z<LENGTH_OVER && y >= 0 && y < HEIGHT_SLICES)
            return this.regions[z*LENGTH_OVER+x][y];
        return null;
    }
    public MeshedRegion getByRegionCoord(int x, int y, int z) {
        x -= this.renderChunkX-RENDER_DISTANCE;
        z -= this.renderChunkZ-RENDER_DISTANCE;
        x+=OFFS_OVER;
        z+=OFFS_OVER;
        if (x>=0&&x<LENGTH_OVER&&z>=0&&z<LENGTH_OVER && y >= 0 && y < HEIGHT_SLICES)
            return this.regions[z*LENGTH_OVER+x][y];
        return null;
    }

    public void resetAll() {
        System.out.println("flush");
        flushRegions();
        this.regionsToUpdate.clear();
        this.renderChunkX = this.renderChunkY = this.renderChunkZ = 0;

        for (int i = 0; this.regions != null && i < this.regions.length; i++) {
            MeshedRegion[] m = this.regions[i];
            for (int y = 0; y < HEIGHT_SLICES; y++) {
                MeshedRegion r = m[y];
                r.needsUpdate = true;
                r.isRenderable = false;
                r.isUpdating = false;
                r.release();
            }
        }
        this.regions = create();
    }
    
    public void setRenderDistance(int i) {
        resetAll();
        RENDER_DISTANCE = i;
        LENGTH_OVER     = (RENDER_DISTANCE + OFFS_OVER) * 2 + 1;
        this.regions = create();
    }

    public void reRender() {
        this.regionsToUpdate.clear();
        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] m = this.regions[i];
            for (int y = 0; y < HEIGHT_SLICES; y++) {
                MeshedRegion r = m[y];
                r.needsUpdate = true;
                r.isRenderable = false;
                r.isUpdating = false;
                r.occlusionResult = 0;
                r.occlusionQueryState = 0;
                r.updateBB();
            }
        }
        for (int i = 0; i < occlQueriesRunning.length; i++) {
            MeshedRegion mr = occlQueriesRunning[i];
            if (mr == null)
                continue;
            mr.occlusionResult = 0;
            mr.occlusionQueryState = 0;
            occlQueriesRunning[i] = null;
        }        
        queriesRunning = 0;
        this.occlCulled = 0;
        renderList.clear();
        shadowRenderList.clear();
    }
    public void flagBlock(int x, int y, int z) {
//        int toRegionX = x >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
//        int toRegionZ = z >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
        int n = 2;
        for (int rx = -n; rx <= n; rx+=n)
            for (int rz = -n; rz <= n; rz+=n) {
                for (int ry = -n; ry <= n; ry+=n) {
                    int regionX2 = (x+rx) >> (RegionRenderer.REGION_SIZE_BITS+Chunk.SIZE_BITS);
                    int regionY2 = (y+ry) >> (RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
                    int regionZ2 = (z+rz) >> (RegionRenderer.REGION_SIZE_BITS+Chunk.SIZE_BITS);
                    MeshedRegion r = getByRegionCoord(regionX2, regionY2, regionZ2);
                    if (r != null) {
                        r.needsUpdate = true;
//                        return;
                    }
                }
//                if (regionX2 != toRegionX || regionZ2 != toRegionZ) {
//                    System.out.println("also flagging neighbour "+regionX2+"/"+regionZ2+ " ("+toRegionX+"/"+toRegionZ+")");
//                }
            }
    }

    public void flagChunk(int x, int z) {
        int n = 1;
        for (int rx = -n; rx <= n; rx += n) {
            int regionX2 = (x + rx) >> (RegionRenderer.REGION_SIZE_BITS);
            for (int rz = -n; rz <= n; rz += n) {
                int regionZ2 = (z + rz) >> (RegionRenderer.REGION_SIZE_BITS);
                for (int ry = 0; ry < RegionRenderer.HEIGHT_SLICES; ry++) {
                    MeshedRegion r = getByRegionCoord(regionX2, ry, regionZ2);
                    if (r != null) {
                        r.needsUpdate = true;
                    }
                }
            }
        }
    }
    
//    ArrayList<MeshedRegion> justrendered = Lists.newArrayList();
//    public void renderMainPost(World world, float fTime, WorldRenderer worldRenderer) {
//        for (MeshedRegion r : this.justrendered) {
//
//            if (r.hasPass(PASS_SOLID)) {
//                r.renderRegion(fTime, PASS_SOLID);
//                this.numV += r.getNumVertices(PASS_SOLID);
//            }
//            if (numV < 2000000) {
//                if (r.hasPass(PASS_LOD)) {
//                    r.renderRegion(fTime, PASS_LOD);
//                    this.numV += r.getNumVertices(PASS_LOD);
//                }
//            }
//        }
//    }
//    public void renderMainPre(World world, float fTime, WorldRenderer worldRenderer) {
//        justrendered.clear();
//        int size = renderList.size();
//        this.occlCulled=0;
//        this.numV = 0;
//        int LOD_DISTANCE = 33; //TODO: move solid/slab blocks out of LOD PASS
//        Shader cur = worldRenderer.terrainShader;
//        for (int dist = 0; dist < 1; dist++)  {
//            for (int i = 0; i < size; i++) {
//                MeshedRegion r = renderList.get(i);
//                if (!r.hasAnyPass()) {
//                    continue;
//                }
//                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
//                    continue;
//                }
////                if ((dist == 0) != (r.distance < LOD_DISTANCE)) continue;
//                if (ENABLE_OCCL && queriesRunning < occlQueriesRunning.length 
//                        && r.distance > MIN_DIST_OCCL 
//                        && r.frustumStates[0] >= MIN_STATE_OCCL) {
//                    if (r.occlusionQueryState == 0 && r.occlusionResult < 1) {
//                        int idx = -1;
//                        int j = 0;
//                        for (; j < this.occlQueriesRunning.length; j++) {
//                            if (this.occlQueriesRunning[j] == null) {
//                                idx = j;
//                                break;
//                            }
//                        }
//                        r.occlusionQueryState = 1;
//                        occlQueriesRunning[idx] = r;
//                        r.queryPos.set(camX, camY, camZ);
//                        occlQueryShader.enable();
//                        GL15.glBeginQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED, occlQueries[idx]);
//                        GL11.glColorMask(false, false, false, false);
//                        Engine.enableDepthMask(false);
//                        r.renderRegion(fTime, PASS_SHADOW_SOLID);
//                        r.renderRegion(fTime, PASS_LOD);
//                        cur.enable();
//                        GL11.glColorMask(true, true, true, true);
//                        Engine.enableDepthMask(true);
//                        GL15.glEndQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED);
//                        queriesRunning++;
//                    }
//                }
//                this.rendered++;  
//                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
//                    this.occlCulled++;
//                    continue;
//                }
//                if (r.hasPass(PASS_SOLID) || r.hasPass(PASS_LOD)) {
//                    justrendered.add(r);
//                }
//                r.renderRegion(fTime, PASS_SOLID);
//                if (numV < 2000000) {
//                    if (r.hasPass(PASS_LOD)) {
//                        r.renderRegion(fTime, PASS_LOD);
//                        this.numV += r.getNumVertices(PASS_LOD);
//                    }
//                }
//            }
////            if (dist == 0) {
////                cur = worldRenderer.terrainShaderFar;
////                cur.enable();
////                GL11.glDisable(GL11.GL_BLEND);
////            }
//        }
//    }

    public void renderMain(World world, float fTime, WorldRenderer worldRenderer) {
        int size = renderList.size();
        this.occlCulled=0;
        this.numV = 0;
        int totalv=0;
        int LOD_DISTANCE = 42; //TODO: move solid/slab blocks out of LOD PASS
        Shader cur = worldRenderer.terrainShader;
        
        boolean bindless = GL.isBindlessSuppported() && Engine.userSettingUseBindless;
        Engine.bindVAO(GLVAO.vaoBlocksBindless);
        if (bindless) {
            buffer.preDraw(GLVAO.vaoBlocksBindless);
        }
        for (int dist = 0; dist < 2; dist++)  {
            for (int i = 0; i < size; i++) {
                MeshedRegion r = renderList.get(i);
                if (!r.hasAnyPass()) {
                    continue;
                }
                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                    continue;
                }
                if ((dist == 1) && (r.distance > LOD_DISTANCE)) continue;
                if (ENABLE_OCCL && queriesRunning < occlQueriesRunning.length 
                        && r.distance > MIN_DIST_OCCL 
                        && r.frustumStates[0] >= MIN_STATE_OCCL) {
                    if (r.occlusionQueryState == 0 && r.occlusionResult < 1) {
                        int idx = -1;
                        int j = 0;
                        for (; j < this.occlQueriesRunning.length; j++) {
                            if (this.occlQueriesRunning[j] == null) {
                                idx = j;
                                break;
                            }
                        }
                        if (bindless && buffer.getDrawCount() > 0) {
                            buffer.render();
                            buffer.preDraw(GLVAO.vaoBlocksBindless);
                        }
                        r.occlusionQueryState = 1;
                        occlQueriesRunning[idx] = r;
                        r.queryPos.set(camX, camY, camZ);
                        occlQueryShader.enable();
                        GL15.glBeginQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED, occlQueries[idx]);
                        GL11.glColorMask(false, false, false, false);
                        Engine.enableDepthMask(false);
                        Engine.bindVAO(GLVAO.vaoBlocksShadow);
                        r.renderRegion(fTime, PASS_SHADOW_SOLID);
//                        r.renderRegion(fTime, PASS_TRANSPARENT);
                        r.renderRegion(fTime, PASS_LOD);
                        Engine.bindVAO(GLVAO.vaoBlocksBindless);
                        cur.enable();
                        GL11.glColorMask(true, true, true, true);
                        Engine.enableDepthMask(true);
                        GL15.glEndQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED);
                        queriesRunning++;
                    }
                }
                this.rendered++;  
                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.occlCulled++;
                    continue;
                }
                if (dist == 0)
                if (r.hasPass(PASS_SOLID)) {
                    //            System.out.println(glGetInteger(GL_DEPTH_FUNC));
                    if (bindless) {
                        r.renderRegionIndirect(buffer, PASS_SOLID);
                    } else {
                        r.renderRegion(fTime, PASS_SOLID);
                    }
                    this.numV += r.getNumVertices(PASS_SOLID);
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        if (bindless) {
                            r.renderRegionIndirect(buffer, PASS_LOD);
                        } else {
                            r.renderRegion(fTime, PASS_LOD);
                        }
                        this.numV += r.getNumVertices(PASS_LOD);
                    }
                    if (numV > 1000000) {
                        break;
                    }
                }
                if (numV > 3000000) {
                    break;
                }
            }
            totalv+=numV;
            numV=0;
//            if (dist == 0) {
//                cur = worldRenderer.terrainShaderFar;
//                cur.enable();
//                GL11.glDisable(GL11.GL_BLEND);
//            }
        }
        if (bindless && buffer.getDrawCount() > 0) {
            buffer.render();
        }
        
        
        numV=totalv;
    }

    public void renderRegions(World world, float fTime, int pass, int nFrustum, int frustumState) {
        GLVAO vao = GLVAO.vaoBlocks;
        if (pass == PASS_SHADOW_SOLID) {
            vao = GLVAO.vaoBlocksShadow;
            if (Game.instance.settings.shadowDrawMode == 1) {
                vao = GLVAO.vaoBlocksShadowTextured;
            }
        }
        int requiredShadowMode = Game.instance.settings.shadowDrawMode;
        boolean bindless = GL.isBindlessSuppported() && Engine.userSettingUseBindless;

        Engine.bindVAO(vao);
        List<MeshedRegion> list = pass == PASS_SHADOW_SOLID ? this.shadowRenderList : this.renderList;
        int size = list.size();
        if (bindless) {
            buffer.preDraw(vao);
        }
        for (int i = 0; i < size; i++) {
            MeshedRegion r = list.get(i);
            if (!r.hasPass(pass)) {
                continue;
            }
            if (r.frustumStates[nFrustum] < frustumState) {
                continue;
            }
            if (r.getShadowDrawMode() != requiredShadowMode) {
                continue;
            }
            if (bindless) {
                r.renderRegionIndirect(buffer, pass);
            } else {
                r.renderRegion(fTime, pass);
            }
//            if (vao == GLVAO.vaoBlocksShadow ){
//                System.out.println("success "+i);
//            }
        }
        if (bindless && buffer.getDrawCount() > 0) {
            buffer.render();
        }
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderRegions");
    }

     void flushRegions() {
        this.occlCulled = 0;
        renderList.clear();
        shadowRenderList.clear();
    }
    
    public void update(WorldClient world, float lastCamX, float lastCamY, float lastCamZ, int xPosP, int zPosP, float fTime) {
        TimingHelper.startSilent(2);
        flushRegions();
        camX=lastCamX;
        camY=lastCamY;
        camZ=lastCamZ;

        rChunkX = GameMath.floor(lastCamX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
        rChunkY = GameMath.floor(lastCamY)>>(RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
        rChunkZ = GameMath.floor(lastCamZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
        
        boolean reposition = false;
        //TODO: only move center every n regions
        if (rChunkX != this.renderChunkX || rChunkZ != this.renderChunkZ) {
            reposition = true;
        }
        MeshThread thread = Engine.regionRenderThread;
        MeshedRegion mFinished = thread.finishTask();
        if (mFinished != null) {
            mFinished.isUpdating = false;
        }
        if (reposition) {
            needsSortingUpdateRenderers = true;
            reposition(rChunkX, rChunkZ);
            for (int i = 0; i < occlQueriesRunning.length; i++) {
                MeshedRegion mr = occlQueriesRunning[i];
                if (mr == null)
                    continue;
                mr.occlusionResult = 0;
                mr.occlusionQueryState = 0;
                occlQueriesRunning[i] = null;
                queriesRunning--;
            }             
        }
//        long n, timespent;
//        timespent = 0L;
//        TimingHelper.startSilent(1);
        
        if (threadedCulling) {
            for (int i = 0; i < 4; i++) {
                threadCtx[i].regionsToUpdate.clear();
                threadCtx[i].renderList.clear();
                threadCtx[i].shadowRenderList.clear();
                threadCtx[i].workedOn=0;
            }
            TimingHelper.start(4);
            worker.work(this);
            TimingHelper.end(4);
            for (int i = 0; i < 4; i++) {
                this.regionsToUpdate.addAll(threadCtx[i].regionsToUpdate);
                this.renderList.addAll(threadCtx[i].renderList);
                this.shadowRenderList.addAll(threadCtx[i].shadowRenderList);
            }
//            for (int i = 0; i < 4; i++) {
//                System.out.println("thread "+i+" worked on "+threadCtx[i].workedOn);
//            }
            
        } else {
            TimingHelper.start(3);
            traverseRenderers();
            TimingHelper.end(3);
        }
//        traverseRenderers(rChunkX, rChunkY, rChunkZ);
//        System.out.println(timespent/1000L);
//        long took = TimingHelper.stopSilent(1);
//        System.out.println("array "+took);
        for (int i = 0; ENABLE_OCCL && i < occlQueriesRunning.length; i++) {
            MeshedRegion mr = occlQueriesRunning[i];
            if (mr == null)
                continue;
            if (mr.isValid && mr.occlusionQueryState == 1) {
                if (mr.queryPos.distanceSq(camX, camY, camZ) < 3) {
                    int done = GL15.glGetQueryObjecti(occlQueries[i], GL15.GL_QUERY_RESULT_AVAILABLE);
                    if (done == 0) {
                        continue;
                    }
                    mr.occlusionResult = 1+GL15.glGetQueryObjecti(occlQueries[i], GL15.GL_QUERY_RESULT);
                    mr.occlFrameSkips = 0;
//                    System.out.println("got result for "+mr+" = "+mr.occlusionResult+ " - "+mr.getNumVertices(0));
                    mr.occlusionQueryState = 0;
                }
            }
            occlQueriesRunning[i] = null;
            queriesRunning--;
        
        }
        if (!renderList.isEmpty()) {
            sortRenderers();
        }
        if (!regionsToUpdate.isEmpty() && !thread.busy()) {
            sortUpdateRenderers();
            //          Region r = Engine.regionLoader.getRegion(m.rX, m.rZ);
            //          if (r != null && r.state == Region.STATE_LOAD_COMPLETE) {
            //          }
            
            for (int i = 0; i < regionsToUpdate.size() && !thread.busy(); i++) {
                MeshedRegion m = regionsToUpdate.get(i);
                if (m.failedCached > 0) {
                    continue;
                }
                if (thread.offer(world, m, renderChunkX, renderChunkZ)) {
                    m.needsUpdate = false;
                    m.failedCached = 0;
                    regionsToUpdate.remove(i--);
                } else {
                    m.failedCached = 10+(i%5);
                }
            }
        }
        if (queriesRunning < 0) {
            System.err.println("queries running < 0!!!!!!!!!!");
        }
    }

    @Override
    public void fromThread(int threadId, int maxThreads) {
        ThreadContext t = this.threadCtx[threadId];
        int perThread = (int) Math.ceil(this.regions.length/(float)maxThreads);
        int start = threadId*perThread;
        int end = start+perThread;
        if (threadId==maxThreads-1)
            end=this.regions.length;
        for (int i = start; i < end; i++) {
            MeshedRegion[] regions = this.regions[i];
            for (int yy = 0; yy < HEIGHT_SLICES; yy++) {
                MeshedRegion m = regions[yy];
                if (m != null) {
                    t.workedOn++;
                    int newDistance = GameMath.distSq3Di(rChunkX, rChunkY, rChunkZ, m.rX, m.rY, m.rZ);
                    m.distance = newDistance;
                    if (m.occlusionQueryState == 0 && m.occlusionResult > 0) {
//                        //m.occlFrameSkips > 100
//                        if (/*queriesRunning < this.occlQueries.length || */m.queryPos.distanceSq(camX, camY, camZ) >= 3) {
//                            m.occlusionResult = 0;
//                            m.occlFrameSkips = 0;
//                        }
                        if (queriesRunning < this.occlQueries.length && (m.queryPos.distanceSq(camX, camY, camZ) >= 3)) {
                            m.occlusionResult = 0;
                            m.occlFrameSkips = 0;
                        }
                    }
                    if (m.isRenderable && m.hasAnyPass()) {
//                        n=System.nanoTime();
                        updateFrustum(m);
//                        timespent+=System.nanoTime()-n;
                        int a = 0;
                        for (;a<4;a++) {
                            if (m.frustumStates[a]>-1) {
                                if (a == 0)
                                    t.renderList.add(m);
                                if (a > 0) {
                                    t.shadowRenderList.add(m);
                                    break;
                                }
                            }
                        }
                    } /* else if (m.ticksSinceFrustumUpdate++>20) { 
                        updateFrustum(m);...
                        */ 
                    if (m.needsUpdate && !m.isUpdating) {
                        m.isUpdating = true;
                        t.regionsToUpdate.add(m);
                        needsSortingUpdateRenderers = true;
                    }
                }
            }
        }
    
    }
    public void traverseRenderers() {

        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] regions = this.regions[i];
            for (int yy = 0; yy < HEIGHT_SLICES; yy++) {
                MeshedRegion m = regions[yy];
                if (m != null) {
                    int newDistance = GameMath.distSq3Di(rChunkX, rChunkY, rChunkZ, m.rX, m.rY, m.rZ);
                    m.distance = newDistance;
                    if (m.occlusionQueryState == 0 && m.occlusionResult > 0) {
//                        //m.occlFrameSkips > 100
//                        if (/*queriesRunning < this.occlQueries.length || */m.queryPos.distanceSq(camX, camY, camZ) >= 3) {
//                            m.occlusionResult = 0;
//                            m.occlFrameSkips = 0;
//                        }
                        if (queriesRunning < this.occlQueries.length && (m.queryPos.distanceSq(camX, camY, camZ) >= 3)) {
                            m.occlusionResult = 0;
                            m.occlFrameSkips = 0;
                        }
                    }
                    if (m.isRenderable && m.hasAnyPass()) {
//                        n=System.nanoTime();
                        updateFrustum(m);
//                        timespent+=System.nanoTime()-n;
                        int a = 0;
                        for (;a<4;a++) {
                            if (m.frustumStates[a]>-1) {
                                if (a == 0)
                                    renderList.add(m);
                                if (a > 0) {
                                    shadowRenderList.add(m);
                                    break;
                                }
                            }
                        }
                    } /* else if (m.ticksSinceFrustumUpdate++>20) { 
                        updateFrustum(m);...
                        */ 
                    if (m.needsUpdate && !m.isUpdating) {
                        m.isUpdating = true;
                        regionsToUpdate.add(m);
                        needsSortingUpdateRenderers = true;
                    }
                }
            }
        }
    }

    /**
     * @param m
     */
    private void updateFrustum(MeshedRegion m) {
        int oof = m.frustumStates[0];
        for (int i = 0; i < 4; i++) {
            m.frustumStates[i] = -1;
        }
        m.frustumStates[0] = Engine.camFrustum.checkFrustum(m.aabb, 27.8f);
        m.frustumStateChanged = (m.frustumStates[0]) != oof;
        if (m.frustumStateChanged) {
            m.occlusionResult = 0;
            if (m.occlusionQueryState == 1) {
                m.occlusionQueryState = 2;
            }
        }
        for (int i = 0; i < 3; i++) {
            int state = Engine.shadowProj.checkFrustum(2-i, m.aabb, 27.8f);
            if (state < 0) {
                break;
            }
            m.frustumStates[3-i] = state;
        }
    }
    public void tickUpdate() {
        for (int i = 0; i < regionsToUpdate.size(); i++) {
            MeshedRegion m = regionsToUpdate.get(i);
            if (m.failedCached > 0) {
                m.failedCached--;
                continue;
            }
        }
        final int MIN_DIST_OCCL = 3;
        for (int i = 0; i < renderList.size(); i++) {
            MeshedRegion r = renderList.get(i);
            if (!r.hasAnyPass()) {
                continue;
            }
            if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                continue;
            }
            if (r.distance > MIN_DIST_OCCL && r.occlusionQueryState == 0 && r.occlusionResult == 1) {
              r.occlFrameSkips++;
              continue;
            }
        }
    }

    private void sortUpdateRenderers() {
        if (needsSortingUpdateRenderers) {
            needsSortingUpdateRenderers = false;
            Collections.sort(regionsToUpdate, compareUpdateRenderers);
        }
    }
    

    private void sortRenderers() {
        Collections.sort(renderList, compareRenderers);
    }
    
    
    // maybe this is faster if we calculate the distance per region once and saving the result _before calling sort_
    protected int sortUpdateRenderersCompare(MeshedRegion o1, MeshedRegion o2) {
        boolean frustum = o1.frustumStates[0] > -1;
        boolean frustum2 = o2.frustumStates[0] > -1;
        if (frustum && !frustum2)
            return -1;
        if (!frustum && frustum2)
            return 1;
        return (o1.distance < o2.distance) ? -1 : ((o1.distance == o2.distance) ? 0 : 1);
    }
    
    
    // maybe this is faster if we calculate the distance per region once and saving the result _before calling sort_
    protected int sortRenderersCompare(MeshedRegion o1, MeshedRegion o2) {
        boolean frustum = o1.frustumStates[0] > -1;
        boolean frustum2 = o2.frustumStates[0] > -1;
        if (frustum && !frustum2)
            return -1;
        if (!frustum && frustum2)
            return 1;
        return (o1.distance < o2.distance) ? -1 : ((o1.distance == o2.distance) ? 0 : 1);
    }

}
