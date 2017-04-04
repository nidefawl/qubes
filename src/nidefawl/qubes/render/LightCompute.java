package nidefawl.qubes.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.WorldClient;

public abstract class LightCompute extends AbstractRenderer {
    public final static int SIZE_OF_STRUCT_LIGHT  = 16*4;
    protected int[]         lightTiles;
    protected int           lightTilesTex;
    protected int           numLights;
    protected int[]         debugResults;
    
    public abstract void updateLights(WorldClient world, float f);

    protected final void updateAndStoreLights(WorldClient world, float fTime, FloatBuffer lightBuf) {
        ArrayList<DynamicLight> lights = world.lights;
        int a = 0;
        int nLights = 0;
        for (; a < lights.size() && a < Engine.MAX_LIGHTS; a++) {
            
            DynamicLight light = lights.get(a);
            
            light.updatePreRender(world, fTime);
            int n = Engine.camFrustum.sphereInFrustum(light.renderPos, light.radius*1.02f);
            if (n >= Frustum.FRUSTUM_INSIDE) {
                nLights++;
                light.store(lightBuf);
                while (lightBuf.position()%16!=0) {
                    lightBuf.put(0);
                }   
            } else {
//                System.out.println("outside!");
            }
        }
        this.numLights = nLights;
    }
    
    @Override
    public void resize(int displayWidth, int displayHeight) {
        int groupsX = displayWidth / 32 + (displayWidth % 32 != 0 ? 1 : 0);
        int groupsY = displayHeight / 32 + (displayHeight % 32 != 0 ? 1 : 0);
        this.lightTiles = new int[] { groupsX, groupsY };
    }
}
