package nidefawl.qubes.render;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.world.World;

public class RegionRenderUpdateTask {

    public int worldInstance;

    Region     region;
    final TesselatorState[] state = new TesselatorState[WorldRenderer.NUM_PASSES];
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
                this.region.doMeshing(w);
                Tess tess = Tess.tessTerrain;
                this.region.renderMeshes(tess, 0);
                tess.copyTo(state[0]);
                this.region.renderMeshes(tess, 1);
                tess.copyTo(state[1]);
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
