package nidefawl.qubes.render.region;

import static org.lwjgl.opengl.GL11.*;
import static nidefawl.qubes.render.WorldRenderer.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class RegionRenderer {
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
    public int                      numRegions;
    public int                      renderChunkX;
    public int                      renderChunkY;
    public int                      renderChunkZ;
    private ArrayList<MeshedRegion> renderList      = new ArrayList<>();
    private ArrayList<MeshedRegion> shadowRenderList      = new ArrayList<>();
    
    private ArrayList<MeshedRegion> regionsToUpdate = new ArrayList<>();
    boolean                         needsSortingUpdateRenderers    = false;
    
    
    TesselatorState                 debug           = new TesselatorState();
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
    int drawMode = -1;
    int drawInstances = 0;
    
    protected static ByteBuffer[] buffers = new ByteBuffer[NUM_PASSES];
    protected static IntBuffer[] intbuffers = new IntBuffer[NUM_PASSES];
    protected static ByteBuffer[] idxByteBuffers = new ByteBuffer[NUM_PASSES];
    protected static IntBuffer[] idxShortBuffers = new IntBuffer[NUM_PASSES];
    
    static void reallocBuffer(int pass, int len) {
        if (buffers[pass] == null || buffers[pass].capacity() < len) {
            if (buffers[pass] != null) {
                buffers[pass] = Memory.reallocByteBufferAligned(buffers[pass], 64, len);
            } else {
                buffers[pass] = Memory.createByteBufferAligned(64, len);
            }
            intbuffers[pass] = buffers[pass].asIntBuffer();
        }
    }
    static void reallocIndexBuffers(int pass, int len) {
        if (BlockFaceAttr.USE_TRIANGLES) {
            if (idxByteBuffers[pass] == null || idxByteBuffers[pass].capacity() < len) {
                if (idxByteBuffers[pass] != null) {
                    idxByteBuffers[pass] = Memory.reallocByteBufferAligned(idxByteBuffers[pass], 64, len);
                } else {
                    idxByteBuffers[pass] = Memory.createByteBufferAligned(64, len);
                }
                idxShortBuffers[pass] = idxByteBuffers[pass].asIntBuffer();
            }
        }
    }

    public void init() {
        initShaders();
        setRenderDistance(Game.instance.settings.chunkLoadDistance>>(REGION_SIZE_BITS));
        IntBuffer intbuf = Engine.glGenBuffers(this.occlQueries.length);
        intbuf.get(this.occlQueries);
    }
    private void releaseShaders() {
        if (occlQueryShader != null) {
            occlQueryShader.release();
            occlQueryShader = null;
        }
    }
    
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_occlQueryShader = assetMgr.loadShader("shaders/basic/occlusion_query");
            releaseShaders();
            occlQueryShader = new_occlQueryShader;
            startup = false;
        } catch (ShaderCompileError e) {
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

    private MeshedRegion getByRegionCoord(int x, int y, int z) {
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
        create();
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
    
    public void setDrawMode(int i) {
        this.drawMode = i;
    }
    public void setDrawInstances(int i) {
        this.drawInstances = i;
    }


    public void renderMain(World world, float fTime, WorldRenderer worldRenderer) {
        int size = renderList.size();
//        PASS_SOLID, 0, Frustum.FRUSTUM_INSIDE
        int drawMode = this.drawMode < 0 ? (BlockFaceAttr.USE_TRIANGLES ? GL11.GL_TRIANGLES : GL11.GL_QUADS) : this.drawMode;
        this.numRegions=0;
        Shader cur = worldRenderer.terrainShader;
        for (int dist = 0; dist < 2; dist++)  {
            for (int i = 0; i < size; i++) {
                MeshedRegion r = renderList.get(i);
                if (!r.hasAnyPass()) {
                    continue;
                }
                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                    continue;
                }
                if ((dist == 0) != (r.distance < 8)) continue;
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
                        r.occlusionQueryState = 1;
                        occlQueriesRunning[idx] = r;
                        r.queryPos.set(camX, camY, camZ);
                        occlQueryShader.enable();
                        GL15.glBeginQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED, occlQueries[idx]);
                        GL11.glColorMask(false, false, false, false);
                        GL11.glDepthMask(false);
                        r.renderRegion(fTime, PASS_SHADOW_SOLID, drawMode, this.drawInstances);
                        r.renderRegion(fTime, PASS_LOD, drawMode, this.drawInstances);
                        cur.enable();
                        GL11.glColorMask(true, true, true, true);
                        GL11.glDepthMask(true);
                        GL15.glEndQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED);
                        queriesRunning++;
                    }
                }
                this.rendered++;  
                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.numRegions++;
                    continue;
                }
                if (r.hasPass(PASS_SOLID)) {
                    r.renderRegion(fTime, PASS_SOLID, drawMode, this.drawInstances);
                }
                if (dist==0&&r.hasPass(PASS_LOD)) {
                    r.renderRegion(fTime, PASS_LOD, drawMode, this.drawInstances);
                }
            }
            if (dist == 0) {
                cur = worldRenderer.terrainShaderFar;
                cur.enable();
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }
    public void renderRegions(World world, float fTime, int pass, int nFrustum, int frustumState) {
        List<MeshedRegion> list = pass == PASS_SHADOW_SOLID ? this.shadowRenderList : this.renderList;
        int size = list.size();
        int drawMode = this.drawMode < 0 ? (BlockFaceAttr.USE_TRIANGLES ? GL11.GL_TRIANGLES : GL11.GL_QUADS) : this.drawMode;
        for (int i = 0; i < size; i++) {
            MeshedRegion r = list.get(i);
            if (r.hasPass(pass) && r.frustumStates[nFrustum] >= frustumState) {
                r.renderRegion(fTime, pass, drawMode, this.drawInstances);
            }
        }
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderRegions");
    }

     void flushRegions() {
        this.numRegions = 0;
        renderList.clear();
        shadowRenderList.clear();
    }
     void putRegion(MeshedRegion r) {
        renderList.add(r);
    }



    public void update(WorldClient world, float lastCamX, float lastCamY, float lastCamZ, int xPosP, int zPosP, float fTime) {
        TimingHelper.startSilent(2);
        flushRegions();
        camX=lastCamX;
        camY=lastCamY;
        camZ=lastCamZ;
        int rChunkX = GameMath.floor(lastCamX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
        int rChunkY = GameMath.floor(lastCamY)>>(RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
        int rChunkZ = GameMath.floor(lastCamZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
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
//        TimingHelper.startSilent(1);
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
                        updateFrustum(m);
                        int a = 0;
                        for (;a<4;a++) {
                            if (m.frustumStates[a]>-1) {
                                if (a == 0)
                                    putRegion(m);
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
    /**
     * @param m
     */
    private void updateFrustum(MeshedRegion m) {
        int oof = m.frustumStates[0];
        for (int i = 0; i < 4; i++) {
            m.frustumStates[i] = -1;
        }
        m.frustumStates[0] = Engine.camFrustum.checkFrustum(m.aabb);
        m.frustumStateChanged = (m.frustumStates[0]) != oof;
        if (m.frustumStateChanged) {
            m.occlusionResult = 0;
            if (m.occlusionQueryState == 1) {
                m.occlusionQueryState = 2;
            }
        }
        for (int i = 0; i < 3; i++) {
            int state = Engine.shadowProj.checkFrustum(2-i, m.aabb);
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

    public void renderDebug(World world, float fTime) {
        Tess.instance.setColor(-1, 200);
        int b=RegionRenderer.REGION_SIZE*Chunk.SIZE;
        int h = World.MAX_WORLDHEIGHT/3*2;
        for (int x = 0; x < b; x+=Chunk.SIZE) {
            for (int z = 0; z < b; z+=Chunk.SIZE) {
                Tess.instance.setColor(-1, 200);
                if (x==0||z==0)
                    Tess.instance.setColor(0xffff00, 200);
                Tess.instance.add(x, 0, z);
                Tess.instance.add(x, h, z);
                Tess.instance.add(z, 0, x);
                Tess.instance.add(z, h, x);
            }
        }
        Tess.instance.draw(GL_LINES, debug);
        glEnable(GL_BLEND);
        debug.bindVBO();
        debug.setAttrPtr();
        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] r = this.regions[i];
            int xOff = r[0].rX << (RegionRenderer.REGION_SIZE_BITS + 4);
            int zOff = r[0].rZ << (RegionRenderer.REGION_SIZE_BITS + 4);
            Shaders.colored.setProgramUniform3f("in_offset", xOff, 0, zOff);
            debug.drawVBO(GL_LINES);
            Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);
        }
    }
}
