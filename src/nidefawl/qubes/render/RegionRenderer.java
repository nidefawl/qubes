package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;

import java.util.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;

public class RegionRenderer {
    public static final int RENDER_DISTANCE     = RegionLoader.LOAD_DIST;
    public static final int LENGTH     = RENDER_DISTANCE*2+1;
    public static final int OFFS_OVER     = 2;
    public static final int LENGTH_OVER     = (RENDER_DISTANCE+OFFS_OVER)*2+1;
    //TODO: abstract render regions
    public static final int RENDER_STATE_INIT     = 0;
    public static final int RENDER_STATE_MESHING  = 1;
    public static final int RENDER_STATE_COMPILED = 2;

    private ArrayList<MeshedRegion> firstPass       = new ArrayList<MeshedRegion>();
    private ArrayList<MeshedRegion> secondPass      = new ArrayList<MeshedRegion>();
//    MeshedRegionTable regions = new MeshedRegionTable(RENDER_DISTANCE*2);
    public int rendered;
    public int numRegions;
    private int renderChunkX;
    private int renderChunkY;
    private int renderChunkZ;

    public void init() {
        this.regions = create();
        MeshedRegion r = getByRegionCoord(0, 0);
        System.out.println(r.rX+"/"+r.rZ);
    }
    
    MeshedRegion[][] regions;
    private MeshedRegion[][] create() {
        MeshedRegion[][] regions = new MeshedRegion[LENGTH_OVER][];
        for (int x = 0; x < regions.length; x++) {
            MeshedRegion[] zRegions = regions[x] = new MeshedRegion[LENGTH_OVER];
            for (int z = 0; z < zRegions.length; z++) {
                MeshedRegion m = new MeshedRegion();
                m.rX = x-(RENDER_DISTANCE+OFFS_OVER);
                m.rZ = z-(RENDER_DISTANCE+OFFS_OVER);
                zRegions[z] = m;
            }
        }
        return regions;
    }
    private MeshedRegion getDirect(int x, int z) {
        return this.regions[x][z];
    }
    //TODO: keep second array around and flipflop
    private void reposition(int rChunkX, int rChunkZ) {
        
        int diffX = rChunkX-this.renderChunkX;
        int diffZ = rChunkZ-this.renderChunkZ;
        
        //TODO: this can be done better
        // we can calculate which regions will fall out of the grid
        HashSet<MeshedRegion> oldRegions = new HashSet<>();
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                oldRegions.add(zRegions[z]);
            }
        }
        MeshedRegion[][] newRegions = create(); //TODO: do not allocate new MeshedRegion where getDirect returns the old one
        for (int x = 0; x < newRegions.length; x++) {
            MeshedRegion[] zRegions = newRegions[x];
            for (int z = 0; z < zRegions.length; z++) {
                int oldX = x+diffX;
                int oldZ = z+diffZ;

                if (oldX>=0&&oldX<LENGTH_OVER&&oldZ>=0&&oldZ<LENGTH_OVER) {
                    MeshedRegion m = getDirect(oldX, oldZ);
                    zRegions[z] = m;
                    oldRegions.remove(m);
                } else {
                    zRegions[z].rX += rChunkX;
                    zRegions[z].rZ += rChunkZ;
                }
            }
        }
        for (MeshedRegion old : oldRegions) {
            old.release();
        }
        this.regions = newRegions;
        this.renderChunkX = rChunkX;
        this.renderChunkZ = rChunkZ;
        
    }
    private MeshedRegion get(int x, int z) {
        return this.regions[x+OFFS_OVER][z+OFFS_OVER];
    }
    private MeshedRegion getByRegionCoord(int x, int z) {
        x -= this.renderChunkX-RENDER_DISTANCE;
        z -= this.renderChunkZ-RENDER_DISTANCE;
        if (x>=0&&x<LENGTH_OVER&&z>=0&&z<LENGTH_OVER)
            return this.regions[x+OFFS_OVER][z+OFFS_OVER];
        return null;
    }
    public void flush() {
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion r = zRegions[z];
                r.renderState = RENDER_STATE_INIT;
                r.isRenderable = false;
                r.release();
            }
        }
    }

    public void reRender() {
        for (int x = 0; x < this.regions.length; x++) {
            MeshedRegion[] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion r = zRegions[z];
                r.renderState = RENDER_STATE_INIT;
                r.isRenderable = false;
            }
        }
    }
    public void flagBlock(int x, int y, int z) {
//        int toRegionX = x >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
//        int toRegionZ = z >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
        int n = 4;
        for (int rx = -n; rx <= n; rx+=n)
            for (int rz = -n; rz <= n; rz+=n) {
                int regionX2 = (x+rx) >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
                int regionZ2 = (z+rz) >> (Region.REGION_SIZE_BITS+Chunk.SIZE_BITS);
                MeshedRegion r = getByRegionCoord(regionX2, regionZ2);
                if (r != null) {
                    r.renderState = RENDER_STATE_INIT;
                }
//                if (regionX2 != toRegionX || regionZ2 != toRegionZ) {
//                    System.out.println("also flagging neighbour "+regionX2+"/"+regionZ2+ " ("+toRegionX+"/"+toRegionZ+")");
//                }
            }
    }
    int drawMode = GL11.GL_QUADS;
    public int getDrawMode() {
//        this.drawMode = GL11.GL_LINES_ADJANCENY
        return drawMode;
    }
    public void setDrawMode(int i) {
        this.drawMode = i;
    }
    
    public void renderFirstPass(World world, float fTime) {
        glDisable(GL_BLEND);
        int size = firstPass.size();
        for (int i = 0; i < size; i++) {
            MeshedRegion r = firstPass.get(i);
            r.renderRegion(fTime, 0, MeshedRegion.LAYER_MAIN, this.drawMode);
            this.rendered += r.getNumVertices(0);
        }
//        for (int i = 0; i < Tess.attributes.length; i++)
//            GL20.glDisableVertexAttribArray(i);

    }
    public void renderSecondPass(World world, float fTime) {
        //TODO: sort by distance
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int size = secondPass.size();
        
        for (int i = 0; i < size; i++) {
            MeshedRegion r = secondPass.get(i);
            r.renderRegion(fTime, 1, MeshedRegion.LAYER_MAIN, this.drawMode);
            this.rendered += r.getNumVertices(1);
        }
//        for (int i = 0; i < Tess.attributes.length; i++)
//            GL20.glDisableVertexAttribArray(i);

        glDisable(GL_BLEND);
    }


    public int getNumRendered() {
        return this.rendered;
    }
    public void flushRegions() {
        this.numRegions = 0;
        firstPass.clear();
        secondPass.clear();
    }
    public void putRegion(MeshedRegion r) {
        this.numRegions++;
        if (r.hasPass(0)) {
            firstPass.add(r);
        }
        if (r.hasPass(1)) {
            secondPass.add(r);
        }
    }
    public void update(float lastCamX, float lastCamY, float lastCamZ, int xPosP, int zPosP, float fTime) {
        flushRegions();
        int rChunkX = GameMath.floor(lastCamX)>>(Chunk.SIZE_BITS+Region.REGION_SIZE_BITS);
//        int rChunkY = GameMath.floor(lastCamY)>>(4+Region.REGION_SIZE_BITS);
        int rChunkZ = GameMath.floor(lastCamZ)>>(Chunk.SIZE_BITS+Region.REGION_SIZE_BITS);
        boolean reposition = false;
        //TODO: only move center every n regions
        if (rChunkX != this.renderChunkX || rChunkZ != this.renderChunkZ) {
            reposition = true;
        }
        RegionRenderThread thread = Engine.regionRenderThread;
        thread.finishTasks();
        if (reposition) {
            reposition(rChunkX, rChunkZ);
        }
        for (int xx = 0; xx < LENGTH; xx++) {
            for (int zz = 0; zz < LENGTH; zz++) {
                MeshedRegion m = this.get(xx, zz);
                if (m != null) {
                    if (m.isRenderable) {
                        putRegion(m);
                    }
                    if (m.renderState == RENDER_STATE_INIT) {
                        if (!thread.busy()) {
                            Region r = Engine.regionLoader.getRegion(m.rX, m.rZ);
                            if (r != null && r.state == Region.STATE_LOAD_COMPLETE) {
                                thread.offer(m, renderChunkX, renderChunkZ);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public List<MeshedRegion> getRegions(int i) {
        return i == 0 ? firstPass : secondPass;
    }
    TesselatorState debug = new TesselatorState();
    public void renderDebug(World world, float fTime) {
        Tess.instance.setColor(-1, 200);
        int b=Region.REGION_SIZE*Chunk.SIZE;
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
            MeshedRegion[] zRegions = this.regions[x];
            for (int z = 0; z < this.regions.length; z++) {
                MeshedRegion r = zRegions[z];
              int xOff = r.rX << (Region.REGION_SIZE_BITS + 4);
              int zOff = r.rZ << (Region.REGION_SIZE_BITS + 4);
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
