package nidefawl.qubes.render;

import java.util.List;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class RegionRenderUpdateTask {
    public final Tess       tess   = new Tess(true);
    public final Mesher     mesher = new Mesher();
    final TesselatorState[] state  = new TesselatorState[WorldRenderer.NUM_PASSES];
    
    public int              worldInstance;
    Region                  region;
    
    public RegionRenderUpdateTask() {
        for (int i = 0; i < state.length; i++) {
            state[i] = new TesselatorState();
        }
    }

    public boolean prepare(Region region) {
        this.region = region;
        region.renderState = Region.RENDER_STATE_MESHING;
        return true;
    }

    public boolean finish(int id) {
        if (!isValid(id)) {
            region.renderState = Region.RENDER_STATE_INIT;
            return true;
        }
        if (this.region.renderState == Region.RENDER_STATE_MESHED) {
            if (!Engine.hasFree()) {
                System.err.println("No free displaylists, waiting for free one before finishing world render task");
                return false;
            }
            this.region.compileDisplayList(state);
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
        if (region.isEmpty()) {
            return true;
        }
        World w = Main.instance.getWorld();
        if (w != null) {
            try {
                List<Mesh> mesh;
                for (int i = 0; i < WorldRenderer.NUM_PASSES; i++) {
                    mesh = mesher.mesh2(w, this.region, i);
                    tess.resetState();
                    tess.setColor(-1, 255);
                    tess.setBrightness(0xf00000);
                    int size = mesh.size();
                    for (int m = 0; m < size; m++) {
                        mesh.get(m).draw(tess);
                    }
                    tess.copyTo(state[i]);
                }
                this.region.renderState = Region.RENDER_STATE_MESHED;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

}
