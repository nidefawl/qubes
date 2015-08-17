package nidefawl.qubes.render;

import java.util.List;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class RegionRenderUpdateTask {
    public final Tess       tess   = new Tess(true);
    public final Mesher     mesher = new Mesher();
    final TesselatorState[] state  = new TesselatorState[WorldRenderer.NUM_PASSES];

    public int              worldInstance;
    Region                  region;
    private boolean         meshed;
    
    public RegionRenderUpdateTask() {
        for (int i = 0; i < this.state.length; i++) {
            this.state[i] = new TesselatorState();
        }
    }

    public boolean prepare(Region region) {
        this.region = region;
        this.meshed = false;
        region.renderState = Region.RENDER_STATE_MESHING;
        return true;
    }

    public boolean finish(int id) {
        if (!isValid(id)) {
            this.region.renderState = Region.RENDER_STATE_INIT;
            return true;
        }
        if (this.meshed) {
            if (!Engine.hasFree()) {
                System.err.println("No free displaylists, waiting for free one before finishing world render task");
                return false;
            }
            long l = System.nanoTime();
            this.region.compileDisplayList(this.state);
            Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
        } else {
            //TODO: flush display list if compile failed, or ignore
        }
        this.region.renderState = Region.RENDER_STATE_COMPILED;
        return true;
    }

    public boolean isValid(int id) {
        return this.worldInstance == id;
    }

    public boolean updateFromThread() {
        if (this.region.isEmpty()) {
            return true;
        }
        World w = Main.instance.getWorld();
        if (w != null) {
            try {
                long l = System.nanoTime();
                this.mesher.mesh(w, this.region);
                Stats.timeMeshing += (System.nanoTime()-l) / 1000000.0D;
                l = System.nanoTime();
                for (int i = 0; i < WorldRenderer.NUM_PASSES; i++) {
                    List<Mesh> mesh = this.mesher.getMeshes(i);
                    this.tess.resetState();
                    this.tess.setColor(-1, 255);
                    this.tess.setBrightness(0xf00000);
                    int size = mesh.size();
                    for (int m = 0; m < size; m++) {
                        mesh.get(m).draw(this.tess);
                    }
                    this.tess.copyTo(this.state[i]);
                }
                Stats.timeRendering += (System.nanoTime()-l) / 1000000.0D;
                this.meshed = true;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

}
