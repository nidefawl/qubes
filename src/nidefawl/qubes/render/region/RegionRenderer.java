package nidefawl.qubes.render.region;

import static org.lwjgl.opengl.GL11.*;

import java.util.*;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class RegionRenderer {
    public static final int RENDER_DISTANCE     = 12;
    public static final int LENGTH     = RENDER_DISTANCE*2+1;
    public static final int OFFS_OVER     = 2;
    public static final int LENGTH_OVER     = (RENDER_DISTANCE+OFFS_OVER)*2+1;
    public static final int HEIGHT_SLICES     = World.MAX_WORLDHEIGHT>>RegionRenderer.SLICE_HEIGHT_BLOCK_BITS;
    //TODO: abstract render regions
    public static final int IN_FRUSTUM_FULLY = 1;
    public static final int IN_FRUSTUM = 0;
    public static final int ALL = -1;

    private ArrayList<MeshedRegion> renderList       = new ArrayList<MeshedRegion>();
//    MeshedRegionTable regions = new MeshedRegionTable(RENDER_DISTANCE*2);
    public int rendered;
    public int numRegions;
    public int renderChunkX;
    public int renderChunkY;
    public int renderChunkZ;

    public void init() {
        this.regions = create();
    }
    
    MeshedRegion[][][] regions;
    private MeshedRegion[][][] create() {
        MeshedRegion[][][] regions = new MeshedRegion[LENGTH_OVER][][];
        for (int x = 0; x < regions.length; x++) {
            MeshedRegion[][] zRegions = regions[x] = new MeshedRegion[LENGTH_OVER][];
            for (int z = 0; z < zRegions.length; z++) {
                MeshedRegion[] ySlices = zRegions[z] = new MeshedRegion[HEIGHT_SLICES];
                for (int y = 0; y < HEIGHT_SLICES; y++) {
                    MeshedRegion m = new MeshedRegion();
                    m.rX = x-(RENDER_DISTANCE+OFFS_OVER);
                    m.rY = y;
                    m.rZ = z-(RENDER_DISTANCE+OFFS_OVER);
                    m.updateBB();
                    ySlices[y] = m;
                }
            }
        }
        return regions;
    }
    private MeshedRegion getDirect(int x, int y, int z) {
        return this.regions[x][z][y];
    }
    private MeshedRegion[] getStack(int x, int z) {
        return this.regions[x][z];
    }
    //TODO: keep second array around and flipflop
    private void reposition(int rChunkX, int rChunkZ) {
        
        int diffX = rChunkX-this.renderChunkX;
        int diffZ = rChunkZ-this.renderChunkZ;
        
        //TODO: this can be done better
        // we can calculate which regions will fall out of the grid
        HashSet<MeshedRegion[]> oldRegions = new HashSet<>();
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[][] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                oldRegions.add(zRegions[z]);
            }
        }
        MeshedRegion[][][] newRegions = create(); //TODO: do not allocate new MeshedRegion where getDirect returns the old one
        for (int x = 0; x < newRegions.length; x++) {
            MeshedRegion[][] zRegions = newRegions[x];
            for (int z = 0; z < zRegions.length; z++) {
                int oldX = x+diffX;
                int oldZ = z+diffZ;

                if (oldX>=0&&oldX<LENGTH_OVER&&oldZ>=0&&oldZ<LENGTH_OVER) {
                    MeshedRegion[] m = getStack(oldX, oldZ);
                    zRegions[z] = m;
                    oldRegions.remove(m);
                } else {
                    MeshedRegion[] m = zRegions[z];
                    for (int y = 0; y < HEIGHT_SLICES; y++) {
                        m[y].rX += rChunkX;
                        m[y].rZ += rChunkZ;
                        m[y].updateBB();
                    }
                }
            }
        }
        for (MeshedRegion[] old : oldRegions) {
            for (int y = 0; y < HEIGHT_SLICES; y++) {
                old[y].release();
                
            }
        }
        this.regions = newRegions;
        this.renderChunkX = rChunkX;
        this.renderChunkZ = rChunkZ;
        
    }
    private MeshedRegion get(int x, int y, int z) {
        return this.regions[x+OFFS_OVER][z+OFFS_OVER][y];
    }
    private MeshedRegion getByRegionCoord(int x, int y, int z) {
        x -= this.renderChunkX-RENDER_DISTANCE;
        z -= this.renderChunkZ-RENDER_DISTANCE;
        x+=OFFS_OVER;
        z+=OFFS_OVER;
        if (x>=0&&x<LENGTH_OVER&&z>=0&&z<LENGTH_OVER && y >= 0 && y < HEIGHT_SLICES)
            return this.regions[x][z][y];
        return null;
    }
    public void flush() {
        System.out.println("flush");
        this.regionsToUpdate.clear();
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[][] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion[] m = zRegions[z];
                for (int y = 0; y < HEIGHT_SLICES; y++) {
                    MeshedRegion r = m[y];
                    r.needsUpdate = true;
                    r.isRenderable = false;
                    r.isUpdating = false;
                    r.release();
                }
            }
        }
    }

    public void reRender() {
        this.regionsToUpdate.clear();
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[][] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion[] m = zRegions[z];
                for (int y = 0; y < HEIGHT_SLICES; y++) {
                    MeshedRegion r = m[y];
                    r.needsUpdate = true;
                    r.isRenderable = false;
                    r.isUpdating = false;
                }
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
//                        System.out.println("flag region "+r.rX+"/"+r.rZ);
                        r.needsUpdate = true;
                    }
                }
            }
        }
    }
    int drawMode = GL11.GL_QUADS;
    int drawInstances = 0;
    public int getDrawMode() {
//        this.drawMode = GL11.GL_LINES_ADJANCENY
        return drawMode;
    }
    public void setDrawMode(int i) {
        this.drawMode = i;
    }
    public void setDrawInstances(int i) {
        this.drawInstances = i;
    }
    
    public void renderRegions(World world, float fTime, int pass, int nFrustum, int frustumState) {
        int size = renderList.size();
        int nSkipped = 0;
        for (int i = 0; i < size; i++) {
            MeshedRegion r = renderList.get(i);
            if (r.hasPass(pass) && r.frustumStates[nFrustum] >= frustumState) {
//                System.out.println(this.rendered);
//                if (this.rendering == 1) {
//                    long sr = GameMath.randomI(r.rX*31+r.rY*13+r.rZ*23);
//                    float fX = (GameMath.randomI(sr+0)%10) / 40.0f;
//                    float fY = (GameMath.randomI(sr+1)%10) / 40.0f;
//                    float fZ = (GameMath.randomI(sr+2)%10) / 40.0f;
//                    Engine.worldRenderer.terrainShader.setProgramUniform4f("terroffset", fX, fY, fZ, 0);
//                }
                r.renderRegion(fTime, pass, this.drawMode, this.drawInstances);

//                if (this.rendering == 1) {
//                    Engine.worldRenderer.terrainShader.setProgramUniform4f("terroffset", 0,0,0,0);    
//                }
                
                this.rendered++;//( += r.getNumVertices(0);   
            } else {
                nSkipped++;
            }
        }
//        System.out.println(nFrustum+": "+nSkipped);

    }

    public void flushRegions() {
        this.numRegions = 0;
        renderList.clear();
    }
    public void putRegion(MeshedRegion r) {
        this.numRegions++;
        renderList.add(r);
    }
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
        }
        if (reposition) {
            needsSorting = true;
            reposition(rChunkX, rChunkZ);
        }
        for (int xx = 0; xx < LENGTH; xx++) {
            for (int zz = 0; zz < LENGTH; zz++) {
                for (int yy = 0; yy < HEIGHT_SLICES; yy++) {
                    MeshedRegion m = this.get(xx, yy, zz);
                    if (m != null) {
                        m.frustumStates[0] = Engine.camFrustum.checkFrustum(m.aabb);
                        for (int i = 0; i < Engine.NUM_PROJECTIONS-1; i++) {
                            m.frustumStates[1+i] = Engine.shadowProj.checkFrustum(i, m.aabb);
                        }
                        if (m.isRenderable) {
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
        }
        if (!regionsToUpdate.isEmpty() && !thread.busy()) {
            sortRenderers();
            //          Region r = Engine.regionLoader.getRegion(m.rX, m.rZ);
            //          if (r != null && r.state == Region.STATE_LOAD_COMPLETE) {
            //          }
            
            for (int i = 0; i < regionsToUpdate.size() && !thread.busy(); i++) {
                MeshedRegion m = regionsToUpdate.get(i);
                if (thread.offer(world, m, renderChunkX, renderChunkZ)) {
                    m.needsUpdate = false;
                    regionsToUpdate.remove(i--);
                }
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

    boolean needsSorting = false;
    Comparator<MeshedRegion> compare = new Comparator<MeshedRegion>() {
        
        @Override
        public int compare(MeshedRegion o1, MeshedRegion o2) {
            return sortCompare(o1, o2);
        }
    };
    ArrayList<MeshedRegion> regionsToUpdate = new ArrayList<>();
    
    public List<MeshedRegion> getRegions(int i) {
        return renderList;
    }

    TesselatorState debug = new TesselatorState();
    public int rendering;
    public static final int REGION_SIZE_BITS      = 1;
    public static final int REGION_SIZE           = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK      = REGION_SIZE - 1;
    public static final int REGION_SIZE_BLOCK_SIZE_BITS    = Chunk.SIZE_BITS+REGION_SIZE_BITS;
    public static final int REGION_SIZE_BLOCKS    = 1 << REGION_SIZE_BLOCK_SIZE_BITS;
    public static final int SLICE_HEIGHT_BLOCK_BITS = 5;
    public static final int SLICE_HEIGHT_BLOCKS    = 1<<SLICE_HEIGHT_BLOCK_BITS;
    public static final int SLICE_HEIGHT_BLOCK_MASK    = SLICE_HEIGHT_BLOCKS-1;
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
//        List<MeshedRegion> regions = Engine.regionRenderer.getRegions(0);
//        for (MeshedRegion r : regions) {
//            glPushMatrix();
//            r.translate();
//            Tess.instance.draw(GL_LINES);
//            glPopMatrix();
//        }
        debug.bindVBO();
        debug.setAttrPtr();
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[][] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion[] r = zRegions[z];
                int xOff = r[0].rX << (RegionRenderer.REGION_SIZE_BITS + 4);
                int zOff = r[0].rZ << (RegionRenderer.REGION_SIZE_BITS + 4);
                Shaders.colored.setProgramUniform3f("in_offset", xOff, 0, zOff);
                debug.drawVBO(GL_LINES);
                Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);
            }
        }
//        for (int x = 0; x < this.regions.length; x++) {
//            MeshedRegion[] zRegions = this.regions[x];
//            for (int z = 0; z < this.regions.length; z++) {
//                MeshedRegion r = zRegions[z];
//                if (r.rX == this.renderChunkX && r.rZ == this.renderChunkZ) {
//                    int xOff = r.rX << (Region.REGION_SIZE_BITS + 4);
//                    int zOff = r.rZ << (Region.REGION_SIZE_BITS + 4);
//                    Shaders.colored.setProgramUniform3f("in_offset", xOff, 0, zOff);
//                    Tess.instance.setColor(0x888800, 0x1f);
//                    Tess.instance.add(0, 120, Region.REGION_SIZE_BLOCKS);
//                    Tess.instance.add(0, 120, 0);
//                    Tess.instance.add(Region.REGION_SIZE_BLOCKS, 120, 0);
//                    Tess.instance.add(Region.REGION_SIZE_BLOCKS, 120, Region.REGION_SIZE_BLOCKS);
//                    Tess.instance.draw(GL_QUADS);
//                    Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);
//                }
//            }
//        }
    }
}
