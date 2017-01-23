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
    int rendererWidth;
    int rendererHeight;
    

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
            r.release();
        resourcesShaders.clear();
        for (IManagedResource r : resourcesFramebuffers)
            r.release();
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
            r.release();
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
            r.release();
        resourcesShadersNew.clear();
    }


    @Override
    public void releaseAll(EResourceType type) {
        switch (type) {
            case FRAMEBUFFER: {

                for (IManagedResource r : resourcesFramebuffers)
                    r.release();
                resourcesFramebuffers.clear();
            }
                return;
            case SHADER: {

                for (IManagedResource r : resourcesShaders)
                    r.release();
                resourcesShaders.clear();
            }
                return;
        }
    }

    public void resizeRenderer(int displayWidth, int displayHeight) {
        this.rendererWidth = displayWidth;
        this.rendererHeight = displayHeight;
        resize(this.rendererWidth, this.rendererHeight);
    }
    public void resize(int displayWidth, int displayHeight) {
        
    }
    
    @Override
    public void preinit() {
        
    }

}
