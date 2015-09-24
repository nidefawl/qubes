package nidefawl.qubes.render.region;

import static org.lwjgl.opengl.GL11.*;
import static nidefawl.qubes.render.WorldRenderer.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class RegionRenderer {
    public static final int REGION_SIZE_BITS            = 1;
    public static final int REGION_SIZE                 = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK            = REGION_SIZE - 1;
    public static final int REGION_SIZE_BLOCK_SIZE_BITS = Chunk.SIZE_BITS + REGION_SIZE_BITS;
    public static final int REGION_SIZE_BLOCKS          = 1 << REGION_SIZE_BLOCK_SIZE_BITS;
    public static final int SLICE_HEIGHT_BLOCK_BITS     = 5;
    public static final int SLICE_HEIGHT_BLOCKS         = 1 << SLICE_HEIGHT_BLOCK_BITS;
    public static final int SLICE_HEIGHT_BLOCK_MASK     = SLICE_HEIGHT_BLOCKS - 1;
    
    public static final int RENDER_DISTANCE = 12;
    public static final int LENGTH          = RENDER_DISTANCE * 2 + 1;
    public static final int OFFS_OVER       = 2;
    public static final int LENGTH_OVER     = (RENDER_DISTANCE + OFFS_OVER) * 2 + 1;
    public static final int HEIGHT_SLICES   = World.MAX_WORLDHEIGHT >> RegionRenderer.SLICE_HEIGHT_BLOCK_BITS;

    
    
    final Comparator<MeshedRegion> compare = new Comparator<MeshedRegion>() {
        @Override
        public int compare(MeshedRegion o1, MeshedRegion o2) {
            return sortCompare(o1, o2);
        }
    };


    public int                      rendered;
    public int                      numRegions;
    public int                      renderChunkX;
    public int                      renderChunkY;
    public int                      renderChunkZ;
    private ArrayList<MeshedRegion> renderList      = new ArrayList<>();
    
    private ArrayList<MeshedRegion> regionsToUpdate = new ArrayList<>();
    boolean                         needsSorting    = false;
    
    
    TesselatorState                 debug           = new TesselatorState();
    protected static ByteBuffer[] buffers = new ByteBuffer[NUM_PASSES];
    protected static IntBuffer[] intbuffers = new IntBuffer[NUM_PASSES];
    protected static ByteBuffer[] idxByteBuffers = new ByteBuffer[NUM_PASSES];
    protected static IntBuffer[] idxShortBuffers = new IntBuffer[NUM_PASSES];
//    = BufferUtils.createByteBuffer(1024*1024*32);
    static void reallocBuffer(int pass, int len) {
        if (buffers[pass] == null || buffers[pass].capacity() < len) {
            int align = ((len+8)/8)*8;
            System.out.println("realloc buffer "+pass+" with "+len+" bytes (aligned to "+align+"  bytes)");
            buffers[pass] = BufferUtils.createByteBuffer(align);
            intbuffers[pass] = buffers[pass].asIntBuffer();
        }
    }
    static void reallocIndexBuffers(int pass, int len) {
        if (BlockFaceAttr.USE_TRIANGLES) {
            if (idxByteBuffers[pass] == null || idxByteBuffers[pass].capacity() < len) {
                int align = ((len+8)/8)*8;
                System.out.println("realloc idx buffer "+pass+" with "+len+" bytes (aligned to "+align+"  bytes)");
                idxByteBuffers[pass] = BufferUtils.createByteBuffer(align);
                idxShortBuffers[pass] = idxByteBuffers[pass].asIntBuffer();
            }
        }
    }

    public void init() {
        this.regions = create();
    }
    
    MeshedRegion[][] regions;
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

        for (int i = 0; i < this.regions.length; i++) {
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

    public void reRender() {
        this.regionsToUpdate.clear();
        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] m = this.regions[i];
            for (int y = 0; y < HEIGHT_SLICES; y++) {
                MeshedRegion r = m[y];
                r.needsUpdate = true;
                r.isRenderable = false;
                r.isUpdating = false;
            }
        }
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
    int drawMode = -1;
    int drawInstances = 0;
    
    public void setDrawMode(int i) {
        this.drawMode = i;
    }
    public void setDrawInstances(int i) {
        this.drawInstances = i;
    }
    
    public void renderRegions(World world, float fTime, int pass, int nFrustum, int frustumState) {
        int size = renderList.size();
        int drawMode = this.drawMode < 0 ? (BlockFaceAttr.USE_TRIANGLES ? GL11.GL_TRIANGLES : GL11.GL_QUADS) : this.drawMode;
        for (int i = 0; i < size; i++) {
            MeshedRegion r = renderList.get(i);
            if (r.hasPass(pass) && r.frustumStates[nFrustum] >= frustumState) {
                r.renderRegion(fTime, pass, drawMode, this.drawInstances);
                this.rendered++;  
            }
        }
    }

     void flushRegions() {
        this.numRegions = 0;
        renderList.clear();
    }
     void putRegion(MeshedRegion r) {
        this.numRegions++;
        renderList.add(r);
    }

    private boolean dirty = true;

    private static int[] BATCH_VBO;

    public void update(WorldClient world, float lastCamX, float lastCamY, float lastCamZ, int xPosP, int zPosP, float fTime) {
        flushRegions();
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
            this.dirty = true;
        }
        
        if (reposition) {
            needsSorting = true;
            reposition(rChunkX, rChunkZ);
        }
        for (int i = 0; i < this.regions.length; i++) {
            MeshedRegion[] regions = this.regions[i];
            for (int yy = 0; yy < HEIGHT_SLICES; yy++) {
                MeshedRegion m = regions[yy];
                if (m != null) {
                    updateFrustum(m);
                    if (m.isRenderable && m.hasAnyPass()) {
                        putRegion(m);
                    }
                    if (m.needsUpdate && !m.isUpdating) {
                        m.isUpdating = true;
                        regionsToUpdate.add(m);
                        needsSorting = true;
                    }
                }
            }
        }
        if (!regionsToUpdate.isEmpty() && !thread.busy()) {
            sortRenderers();
            //          Region r = Engine.regionLoader.getRegion(m.rX, m.rZ);
            //          if (r != null && r.state == Region.STATE_LOAD_COMPLETE) {
            //          }
            
            for (int i = 0; i < regionsToUpdate.size() && !thread.busy(); i++) {
                MeshedRegion m = regionsToUpdate.get(i);
                if (m.failedCached > 0) {
                    continue;
                }
//                System.out.println("put "+m);
                if (thread.offer(world, m, renderChunkX, renderChunkZ)) {
                    m.needsUpdate = false;
                    m.failedCached = 0;
                    regionsToUpdate.remove(i--);
                } else {
                    m.failedCached = 10+(i%5);
                }
            }
        }
    }
    /**
     * @param m
     */
    private void updateFrustum(MeshedRegion m) {
        for (int i = 0; i < 4; i++) {
            m.frustumStates[i] = -1;
        }
        m.frustumStates[0] = Engine.camFrustum.checkFrustum(m.aabb);
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
    }
    
    private void sortRenderers() {
        if (needsSorting) {
            needsSorting = false;
            Collections.sort(regionsToUpdate, compare);
        }
    }
    
    protected int sortCompare(MeshedRegion o1, MeshedRegion o2) {
        boolean frustum = o1.frustumStates[0] > -1;
        boolean frustum2 = o2.frustumStates[0] > -1;
        if (frustum && !frustum2)
            return -1;
        if (!frustum && frustum2)
            return 1;
        int dist1 = GameMath.distSq3Di(o1.rX, o1.rY, o1.rZ, renderChunkX, renderChunkY, renderChunkZ);
        int dist2 = GameMath.distSq3Di(o2.rX, o2.rY, o2.rZ, renderChunkX, renderChunkY, renderChunkZ);
        return (dist1 < dist2) ? -1 : ((dist1 == dist2) ? 0 : 1);
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
