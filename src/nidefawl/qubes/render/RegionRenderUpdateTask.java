package nidefawl.qubes.render;

import java.util.ArrayList;
import java.util.List;

import nidefawl.qubes.Main;
import nidefawl.qubes.chunk.Region;
import static nidefawl.qubes.chunk.Region.*;
import static nidefawl.qubes.render.MeshedRegion.*;
import static nidefawl.qubes.render.WorldRenderer.*;
import nidefawl.qubes.chunk.RegionCache;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class RegionRenderUpdateTask {
    public final Mesher     mesher = new Mesher();
    public final RegionCache cache = new RegionCache();
    final Tess[] tess  = new Tess[NUM_PASSES*NUM_LAYERS];

    public int              worldInstance;
    private boolean         meshed;
    private int x;
    private int z;
    private MeshedRegion mr;
    
    public RegionRenderUpdateTask() {
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
            if (!Engine.hasFree()) {
                System.err.println("No free displaylists, waiting for free one before finishing world render task");
                return false;
            }
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
                long l = System.nanoTime();
                this.mesher.mesh(w, this.cache);
                Stats.timeMeshing += (System.nanoTime()-l) / 1000000.0D;
                l = System.nanoTime();
                for (int j = 0; j < NUM_LAYERS; j++) {
                    for (int i = 0; i < NUM_PASSES; i++) {
                        int idx = i+NUM_PASSES*j;
                        Tess tess = this.tess[idx];
                        List<Mesh> mesh = this.mesher.getMeshes(j, i);
                        tess.resetState();
                        tess.setColor(-1, 255);
                        tess.setBrightness(0xf00000);
                        int size = mesh.size();
                        for (int m = 0; m < size; m++) {
                            Mesh face = mesh.get(m);
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
