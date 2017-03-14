package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;

import java.util.List;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.GLVAO;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.world.World;

public class RegionRendererVK extends RegionRenderer implements IRenderComponent {

    @Override
    public void renderMain(World world, float fTime) {
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
