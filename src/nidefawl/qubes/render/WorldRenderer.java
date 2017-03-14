package nidefawl.qubes.render;

import java.util.HashMap;
import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.path.PathPoint;
import nidefawl.qubes.render.impl.gl.WorldRendererGL;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public abstract class WorldRenderer extends AbstractRenderer {
    public static final int NUM_PASSES        = 4;
    public static final int PASS_SOLID        = 0;
    public static final int PASS_TRANSPARENT  = 1;
    public static final int PASS_SHADOW_SOLID = 2;
    public static final int PASS_LOD          = 3;
    public static final String getPassName(int i) {
        switch (i) {
            case PASS_SOLID:
                return "Main";
            case PASS_TRANSPARENT:
                return "Transparent";
            case PASS_SHADOW_SOLID:
                return "Shadow";
            case PASS_LOD:
                return "LOD";
        }
        return "PASS_"+i;
    }
    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();
    public HashMap<Integer, List<PathPoint>> debugPaths = new HashMap<>();

    public int                               rendered;
    final public static int HALF_EXTRA_RENDER = 0;
    final public static int EXTRA_RENDER = 1;//(HALF_EXTRA_RENDER*2)*(HALF_EXTRA_RENDER*2);
    final public static float RDIST = 4;
    
    public int getNumRendered() {
        return this.rendered;
    }


    public boolean isNormalMappingActive() {
        return Engine.RENDER_SETTINGS.normalMapping > 0 && !Game.VR_SUPPORT;
    }


    public abstract void initShaders();
    public abstract void tickUpdate();


    public void renderEntities(World world, int passShadowSolid, float fTime, int i) {
    }

}
