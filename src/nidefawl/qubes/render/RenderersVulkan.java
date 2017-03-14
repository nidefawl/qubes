package nidefawl.qubes.render;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.EngineInitSettings;
import nidefawl.qubes.gl.ShadowProjector;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.impl.vk.QModelBatchedRenderVK;
import nidefawl.qubes.render.impl.gl.*;
import nidefawl.qubes.render.impl.vk.*;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.ShaderBuffer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.VKContext;

public class RenderersVulkan extends Renders {
    public static WorldRendererVK  worldRenderer;
    public static SkyRendererVK  skyRenderer;
    public static ShadowRendererVK  shadowRenderer;
    public static BlurRendererVK   blurRenderer;
    public static FinalRendererVK  outRenderer;
    public static RegionRendererVK regionRenderer;
    public static CubeParticleRendererVK  particleRenderer;
    public static LightComputeVK  lightCompute;
    public static QModelBatchedRenderVK renderBatched;

    public RenderersVulkan(VKContext vkContext, EngineInitSettings init) {
        components.clear();
        if (init.initShadowRenderer || init.initShadowProj) {
            Engine.shadowProj = shadowProj = addComponent(new ShadowProjector());
        }
        if (init.initShadowRenderer) {
            Engine.shadowRenderer = shadowRenderer = addComponent(new ShadowRendererVK());
        }
        if (init.initWorldRenderer) {
            Engine.worldRenderer = worldRenderer = addComponent(new WorldRendererVK());
            Engine.regionRenderer = regionRenderer = addComponent(new RegionRendererVK());
            Engine.particleRenderer = particleRenderer = addComponent(new CubeParticleRendererVK());
        }
        if (init.initBlurRenderer) {
            Engine.blurRenderer = blurRenderer = addComponent(new BlurRendererVK());
        }
        if (init.initLightCompute) {
            Engine.lightCompute = lightCompute = addComponent(new LightComputeVK());

        }
        if (init.initSkyRenderer) {
            Engine.skyRenderer = skyRenderer = addComponent(new SkyRendererVK());
        }
        if (init.initFinalRenderer) {
            Engine.outRenderer = outRenderer = addComponent(new FinalRendererVK());
        }
        if (init.initModelRenderer) {
            Engine.renderBatched = renderBatched = addComponent(new QModelBatchedRenderVK());
        }
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            r.preinit();
        }
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            r.init();
        }
        //        for (int i = 0; i < components.size(); i++) {
        //            IRenderComponent r = components.get(i);
        //            if (r instanceof AbstractRenderer) {
        //                ((AbstractRenderer) r).resize(Game.displayWidth, Game.displayHeight);    
        //            }
        //        }
    }

}
