package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.impl.gl.ShadowRendererGL;
import nidefawl.qubes.render.impl.vk.ShadowRendererVK;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public abstract class ShadowRenderer extends AbstractRenderer {

    //results between those 2 modes are equal
    // the geom shader mode uses geometry shader to instanciate the terrain
    // reducing 3*(terrain_in_furstum slices) draw+bind loops to 1 draw + bind loop for the biggest frustum
    public static final int MULTI_DRAW             = 0; // FASTES, GL 3.x 
    public static final int MULTI_DRAW_TEXUTED     = 1; // as 0 + textures to discard transparent pixels, GL 3.x 
    public static final int MAX_SHADOW_RENDER_MODE     = 2; 
    

    protected int renderMode = -1;
    protected int SHADOW_BUFFER_SIZE = 1024*4;



    public void init() {
        this.renderMode = Engine.RENDER_SETTINGS.shadowDrawMode>MULTI_DRAW_TEXUTED||Engine.RENDER_SETTINGS.shadowDrawMode<0?MULTI_DRAW:Engine.RENDER_SETTINGS.shadowDrawMode;
        SHADOW_BUFFER_SIZE = 1024*4;
    }

    public int getTextureSize() {
//        return GameBase.VR_SUPPORT?1024*1:1024*4;
        return SHADOW_BUFFER_SIZE;
    }

    public abstract void renderShadowPass(World world, float fTime);

    public abstract void tickUpdate();

}
