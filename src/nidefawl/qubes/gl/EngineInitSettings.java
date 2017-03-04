package nidefawl.qubes.gl;

public class EngineInitSettings {
    protected EngineInitSettings() {
        set();
    }
    public final static EngineInitSettings INIT_ALL = new EngineInitSettings() {
        @Override
        protected void set() {
            inverseZBuffer = true;
            inverseClipspaceYOpengl = false;
            initShadowRenderer = true;
            initBlurRenderer = true;
            initWorldRenderer = true;
            initLightCompute = true;
            initSkyRenderer = true;
            initFinalRenderer = true;
            initModelRenderer = true;
        }
    };
    public final static EngineInitSettings INIT_NONE = new EngineInitSettings() {
        @Override
        protected void set() {
            initShadowRenderer = false;
            initBlurRenderer = false;
            initWorldRenderer = false;
            initLightCompute = false;
            initSkyRenderer = false;
            initFinalRenderer = false;
            initModelRenderer = false;
        }
    };
    public final static EngineInitSettings INIT_SKY_FINAL = new EngineInitSettings() {
        @Override
        protected void set() {
            initShadowRenderer = false;
            initBlurRenderer = true;
            initWorldRenderer = false;
            initLightCompute = false;
            initSkyRenderer = true;
            initFinalRenderer = true;
            initModelRenderer = false;
        }
    };
    protected void set() {
        
    }
    public boolean initShadowRenderer;
    public boolean initBlurRenderer;
    public boolean initWorldRenderer;
    public boolean initLightCompute;
    public boolean initSkyRenderer;
    public boolean initFinalRenderer;
    public boolean initModelRenderer;
    public boolean inverseZBuffer;
    public boolean isVulkan;
    public boolean inverseClipspaceYOpengl;
    public boolean initShadowProj;
    
    public EngineInitSettings setVulkan(boolean isVulkan) {
        this.isVulkan = isVulkan;
        return this;
    }
    public EngineInitSettings setInverseZ() {
        this.inverseZBuffer = true;
        return this;
    }
    public EngineInitSettings setInverseYOpengl() {
        this.inverseClipspaceYOpengl = true;
        return this;
    }
}
