package nidefawl.qubes.render;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.EngineInitSettings;
import nidefawl.qubes.gl.ShadowProjector;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.impl.gl.QModelBatchedRenderGL;
import nidefawl.qubes.render.impl.gl.*;
import nidefawl.qubes.shader.ShaderBuffer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.IRenderComponent;

public class RenderersGL extends Renders {
    public static WorldRendererGL  worldRenderer;
    public static SkyRendererGL  skyRenderer;
    public static ShadowRendererGL  shadowRenderer;
    public static BlurRendererGL   blurRenderer;
    public static FinalRendererGL  outRenderer;
    public static RegionRendererGL regionRenderer;
    public static CubeParticleRenderer  particleRenderer;
    public static LightComputeGL  lightCompute;
    public static QModelBatchedRender renderBatched;

    public RenderersGL(EngineInitSettings init) {
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            r.release();
        }
        components.clear();
        if (init.initShadowRenderer || init.initShadowProj) {
            Engine.shadowProj = shadowProj = addComponent(new ShadowProjector());
        }
        if (init.initShadowRenderer) {
            Engine.shadowRenderer = shadowRenderer = addComponent(new ShadowRendererGL());
        }
        if (init.initWorldRenderer) {
            Engine.worldRenderer = worldRenderer = addComponent(new WorldRendererGL());
            Engine.regionRenderer = regionRenderer = addComponent(new RegionRendererGL());
            Engine.particleRenderer = particleRenderer = addComponent(new CubeParticleRendererGL());
        }
        if (init.initBlurRenderer) {
            Engine.blurRenderer = blurRenderer = addComponent(new BlurRendererGL());
        }
        if (init.initLightCompute) {
            Engine.lightCompute = lightCompute = addComponent(new LightComputeGL());

        }
        if (init.initSkyRenderer) {
            Engine.skyRenderer = skyRenderer = addComponent(new SkyRendererGL());
        }
        if (init.initFinalRenderer) {
            Engine.outRenderer = outRenderer = addComponent(new FinalRendererGL());
        }
        if (init.initModelRenderer) {
            Engine.renderBatched = renderBatched = addComponent(new QModelBatchedRenderGL());
        }
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            r.preinit();
        }
        Shaders.init();
        ShaderBuffer.init();
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
