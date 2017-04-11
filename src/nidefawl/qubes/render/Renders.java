package nidefawl.qubes.render;

import nidefawl.qubes.gl.ShadowProjector;
import nidefawl.qubes.models.render.impl.vk.QModelBatchedRenderVK;
import nidefawl.qubes.render.impl.vk.*;
import nidefawl.qubes.util.FastArrayList;
import nidefawl.qubes.util.IRenderComponent;

public class Renders {
    public static ShadowProjector shadowProj;

    final FastArrayList<IRenderComponent> components = new FastArrayList<>(16);

    public void resizeRenderers(int displayWidth, int displayHeight) {
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            if (r instanceof AbstractRenderer) {
//                if (r instanceof BlurRendererVK)
//                    continue;
//                if (r instanceof LightComputeVK)
//                    continue;
//                if (r instanceof RegionRendererVK)
//                    continue;
//                if (r instanceof SkyRendererVK)
//                    continue;
//                if (r instanceof WorldRendererVK)
//                    continue;
//                if (r instanceof ShadowRendererVK)
//                    continue;
//                if (r instanceof QModelBatchedRenderVK)
//                    continue;
//                if (r instanceof CubeParticleRendererVK)
//                    continue;
                System.out.println("Resize "+r.getClass()+", "+System.currentTimeMillis());
                ((AbstractRenderer) r).resizeRenderer(displayWidth, displayHeight);    
            }
        }
    }
    protected <T> T addComponent(IRenderComponent component) {
        components.add((IRenderComponent) component);
        return (T) component;
    }
    public void release() {
        for (int i = 0; i < components.size(); i++) {
            IRenderComponent r = components.get(i);
            r.release();
        }
    }
    public void init() {
    }
    public void preinit() {
    }
    public void reinit() {
    }
}
