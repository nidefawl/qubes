/**
 * 
 */
package nidefawl.qubes.render;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.util.*;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public abstract class AbstractRenderer implements IResourceManager, IRenderComponent {
    boolean                recompileShaders    = false;
    List<IManagedResource> resourcesShaders    = Lists.newArrayList();
    List<IManagedResource> resourcesShadersNew = Lists.newArrayList();
    List<IManagedResource> resourcesFramebuffers      = Lists.newArrayList();
    protected int rendererWidth;
    protected int rendererHeight;
    protected int downsample = 1;
    

    @Override
    public void addResource(IManagedResource r) {
        switch (r.getType()) {
            case SHADER:
                if (recompileShaders) {
                    resourcesShadersNew.add(r);
                } else {
                    resourcesShaders.add(r);
                }
                break;
            default:
            case FRAMEBUFFER:
                resourcesFramebuffers.add(r);
        }
    }

    @Override
    public void release() {
        for (IManagedResource r : resourcesShaders)
            r.destroy();
        resourcesShaders.clear();
        for (IManagedResource r : resourcesFramebuffers)
            r.destroy();
        resourcesFramebuffers.clear();
    }

    /**
     * Called before shaders recompiled
     */
    public void pushCurrentShaders() {
        recompileShaders = true;
    }

    /**
     * Called on successful shader recompilation
     */
    public void popNewShaders() {
        recompileShaders = false;
        for (IManagedResource r : resourcesShaders)
            r.destroy();
        resourcesShaders.clear();
        resourcesShaders.addAll(resourcesShadersNew);
        resourcesShadersNew.clear();
    }

    /**
     * Called on failed shader recompilation
     */
    public void releaseNewShaders() {
        recompileShaders = false;
        for (IManagedResource r : resourcesShadersNew)
            r.destroy();
        resourcesShadersNew.clear();
    }


    @Override
    public void releaseAll(EResourceType type) {
        switch (type) {
            case FRAMEBUFFER: {

                for (IManagedResource r : resourcesFramebuffers)
                    r.destroy();
                resourcesFramebuffers.clear();
            }
                return;
            case SHADER: {

                for (IManagedResource r : resourcesShaders)
                    r.destroy();
                resourcesShaders.clear();
            }
                return;
        }
    }
    public void resizeRenderer(int displayWidth, int displayHeight) {

        int[] blurSize = GameMath.downsample(displayWidth, displayHeight, downsample);
        this.rendererWidth = blurSize[0];
        this.rendererHeight = blurSize[1];
        resize(this.rendererWidth, this.rendererHeight);
    }
    public abstract void resize(int displayWidth, int displayHeight);
    public void resize() {
        resize(this.rendererWidth, this.rendererHeight);
    }
    
    @Override
    public void preinit() {
        
    }
    
    public void setDownsample(int downsample) {
        this.downsample = downsample;
    }

}
