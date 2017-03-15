package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.render.WorldRenderer.*;

import java.util.List;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;

public class RegionRendererVK extends RegionRenderer implements IRenderComponent {

    public void renderMain(VkCommandBuffer commandBuffer, World world, float fTime) {
        int size = renderList.size();
        this.occlCulled=0;
        this.numV = 0;
        int totalv=0;
        int LOD_DISTANCE = 16; //TODO: move solid/slab blocks out of LOD PASS
        for (int dist = 0; dist < 2; dist++)  {
            for (int i = 0; i < size; i++) {
                MeshedRegion r = renderList.get(i);
                if (!r.hasAnyPass()) {
                    continue;
                }
                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                    continue;
                }
                if ((dist == 1) && (r.distance > LOD_DISTANCE)) continue;

                this.rendered++;  
                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.occlCulled++;
                    continue;
                }
                if (dist == 0)
                if (r.hasPass(PASS_SOLID)) {
                    //            System.out.println(glGetInteger(GL_DEPTH_FUNC));
                    r.renderRegionVK(commandBuffer, fTime, PASS_SOLID);
                    this.numV += r.getNumVertices(PASS_SOLID);
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        r.renderRegionVK(commandBuffer, fTime, PASS_LOD);
                        this.numV += r.getNumVertices(PASS_LOD);
                    }
                    if (numV > 1000000) {
                        break;
                    }
                }
                if (numV > 3000000) {
                    break;
                }
            }
            totalv+=numV;
            numV=0;
//            if (dist == 0) {
//                cur = worldRenderer.terrainShaderFar;
//                cur.enable();
//            }
        }
//        if (occlTestThisFrame > 0) {
//            System.out.println(occlTestThisFrame);
//        }
//        
        numV=totalv;
    
    }

    public void renderRegions(VkCommandBuffer commandBuffer, World world, float fTime, int pass, int nFrustum, int frustumState) {
        int requiredShadowMode = Game.instance.settings.renderSettings.shadowDrawMode;
        List<MeshedRegion> list = pass == PASS_SHADOW_SOLID ? this.shadowRenderList : this.renderList;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            MeshedRegion r = list.get(i);
            if (!r.hasPass(pass)) {
                continue;
            }
            if (r.frustumStates[nFrustum] < frustumState) {
                continue;
            }
            if (r.getShadowDrawMode() != requiredShadowMode) {
                continue;
            }
            r.renderRegionVK(commandBuffer, fTime, pass);
        }
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderRegions");
    
    }

    @Override
    public void updateOcclQueries() {
    }

}
