package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;

import java.util.List;

import nidefawl.qubes.BootClient;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class MeshUpdateTask {
    public final Mesher     mesher = new Mesher();
    public final ChunkRenderCache ccache = new ChunkRenderCache();
    final Tess[] tess  = new Tess[NUM_PASSES];

    public int              worldInstance;
    private boolean         meshed;
    private MeshedRegion mr;
    
    public MeshUpdateTask() {
        for (int i = 0; i < this.tess.length; i++) {
            this.tess[i] = new Tess(true);
        }
    }

    public boolean prepare(WorldClient world, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        if (this.ccache.cache(world, mr, renderChunkX, renderChunkZ)) {
            this.mr = mr;
            mr.renderState = RegionRenderer.RENDER_STATE_MESHING;
            return true;
        } else {
//            System.out.println("cannot render "+mr.rX+"/"+mr.rZ);
        }
        return false;
    }

    public boolean finish(int id) {
        if (!isValid(id)) {
            mr.renderState = RegionRenderer.RENDER_STATE_INIT;
            return true;
        }
        if (this.meshed) {
            long l = System.nanoTime();
            this.mr.compileDisplayList(this.tess);
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
        this.mr.renderState = RegionRenderer.RENDER_STATE_COMPILED;
        return true;
    }

    public boolean isValid(int id) {
        return this.worldInstance == id;
    }

    public boolean updateFromThread() {
//        if (this.mr.isEmpty()) {
//            return true;
//        }
        World w = BootClient.instance.getWorld();
        if (w != null) {
            try {
              int xOff = this.mr.rX << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS);
              int yOff = this.mr.rY << (RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
              int zOff = this.mr.rZ << (RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS);
                long l = System.nanoTime();
                this.mesher.mesh(w, this.ccache, this.mr.rY);
                Stats.timeMeshing += (System.nanoTime()-l) / 1000000.0D;
                l = System.nanoTime();
                for (int i = 0; i < NUM_PASSES; i++) {
                    Tess tess = this.tess[i];
                    List<TerrainQuad> mesh = this.mesher.getMeshes(i);
                    tess.resetState();
                    if (i != 2) {
                        tess.setColor(-1, 255);
                        tess.setBrightness(0xf00000);
                    }
                    tess.setOffset(xOff, yOff, zOff);
                    int size = mesh.size();
                    for (int m = 0; m < size; m++) {
                        TerrainQuad face = mesh.get(m);
                        if (i == 2) {
                            face.drawBasic(tess);
                        } else {
                            face.draw(tess);
                        }
                    }
                }
            
                Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
                this.meshed = true;
                return true;
            } catch (Exception e) {
                BootClient.instance.setException(new GameError("Error while updating region", e));
            }
            return false;
        }
        return false;
    }

}
