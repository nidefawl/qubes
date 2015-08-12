package nidefawl.qubes.render;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.world.World;

public class RegionRenderUpdateTask {

    public int worldInstance;

    Region     region;

    public boolean prepare(Region region) {
        this.region = region;
        region.renderState = Region.RENDER_STATE_MESHING;
        return true;
    }

    public boolean finish(int id) {
        System.out.println("finish region !");
        this.region.renderState = Region.RENDER_STATE_MESHED;
        //TODO: add mesh draw + display list compile here
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
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

}
