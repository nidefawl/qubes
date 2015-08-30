package nidefawl.qubes.meshing;

import java.util.List;

import nidefawl.qubes.Main;

import static nidefawl.qubes.render.WorldRenderer.*;
import static nidefawl.qubes.render.region.MeshedRegion.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionCache;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;

public class MeshUpdateTask {
    public final Mesher     mesher = new Mesher();
    public final RegionCache cache = new RegionCache();
    final Tess[] tess  = new Tess[NUM_PASSES];

    public int              worldInstance;
    private boolean         meshed;
    private int x;
    private int z;
    private MeshedRegion mr;
    
    public MeshUpdateTask() {
        for (int i = 0; i < this.tess.length; i++) {
            this.tess[i] = new Tess(true);
        }
    }

    public boolean prepare(MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        this.cache.flush();
        if (Engine.regionLoader.cacheRegions(mr.rX, mr.rZ, renderChunkX, renderChunkZ, this.cache)) {
            if (mr.rX == 0 &&mr.rZ== 0 ) {
                for (int a = 0; a < cache.regions.length; a++) {
                    System.out.println(""+a+" = "+cache.regions[a]);
                }
            }
            this.mr = mr;
            this.x = mr.rX; 
            this.z = mr.rZ;
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
        this.mr.xNeg = this.cache.get(-1, 0) != null;
        this.mr.xPos = this.cache.get(1, 0) != null;
        this.mr.zNeg = this.cache.get(0, -1) != null;
        this.mr.zPos = this.cache.get(0, 1) != null;
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
        World w = Main.instance.getWorld();
        if (w != null) {
            try {
              int xOff = this.mr.rX << (Region.REGION_SIZE_BITS + Chunk.SIZE_BITS);
              int zOff = this.mr.rZ << (Region.REGION_SIZE_BITS + Chunk.SIZE_BITS);
                long l = System.nanoTime();
                this.mesher.mesh(w, this.cache);
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
                    tess.setOffset(xOff, 0, zOff);
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
                Main.instance.setException(new GameError("Error while updating region", e));
            }
            return false;
        }
        return false;
    }

}
