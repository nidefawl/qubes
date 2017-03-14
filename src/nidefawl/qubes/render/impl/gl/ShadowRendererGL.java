package nidefawl.qubes.render.impl.gl;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.ShadowRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class ShadowRendererGL extends ShadowRenderer {

    public ShadowRendererGL() {
    }

    public Shader   shadowShader;
    private boolean startup = true;
    public final String[] shaderNames = new String[] {
            "shadow/shadow_solid",
            "shadow/shadow_textured",
    };
    private FrameBuffer fbShadow;


    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            pushCurrentShaders();
            Shader shadow = assetMgr.loadShader(this, shaderNames[renderMode]);
            // may never reach this point when shader fails to compile
            // if that happens the catch clause will dealloc any new shaders
            // _only_ if it reaches this point we want to deallocate the old shaders
            popNewShaders();
            shadowShader = shadow;
            shadowShader.enable();
            shadowShader.setProgramUniform1i("blockTextures", 0);
            shadowShader.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
            Shader.disable();
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }
    @Override
    public void init() {
        super.init();
        initShaders();
    }
    
    public void renderMultiPass(World world, float fTime) {
//              glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        float mult = Engine.isInverseZ?-1:1;
        glPolygonOffset(1.1f*mult, 2.f*mult);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        Engine.setViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        final int shadowPass = 2;
//      Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
//    Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 1, Frustum.FRUSTUM_INSIDE);

        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 0); //TODO: FRUSTUM CULLING

        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f*mult, 2.f*mult);

        Engine.setViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 1); //TODO: FRUSTUM CULLING

        shadowShader.enable();
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(1.4f*mult, 2.f*mult);

        Engine.setViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 2); //TODO: FRUSTUM CULLING
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);

//        FrameBuffer.unbindFramebuffer();

//        Shader.disable();

//        Engine.setDefaultViewport();
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
//        glEnable(GL_CULL_FACE);
    }

    public void renderMultiPassTextured(World world, float fTime) {
//              glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        float mult = Engine.isInverseZ?-1:1;
        glPolygonOffset(1.1f*mult, 2.f*mult);
        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        Engine.setViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        shadowShader.setProgramUniform1i("blockTextures", 0);

        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
//        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 0); //TODO: FRUSTUM CULLING
        
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f*mult, 2.f*mult);

        Engine.setViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 1); //TODO: FRUSTUM CULLING

        shadowShader.enable();
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(1.4f*mult, 2.f*mult);

        Engine.setViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        RenderersGL.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 2); //TODO: FRUSTUM CULLING
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);

//        FrameBuffer.unbindFramebuffer();

//        Shader.disable();

//        Engine.setDefaultViewport();
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
//        glEnable(GL_CULL_FACE);
    }

    @Override
    public void renderShadowPass(World world, float fTime) {
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("ShadowPass");
        if (this.renderMode == MULTI_DRAW_TEXUTED) {
            renderMultiPassTextured(world, fTime);
        } else {
            renderMultiPass(world, fTime);
        }
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }

    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        SHADOW_BUFFER_SIZE = getTextureSize();
        this.fbShadow = new FrameBuffer(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
        this.fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        this.fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 1F, 0F, 0F);
        this.fbShadow.setShadowBuffer();
        this.fbShadow.setup(this);
    }

    public int getDepthTex() {
        return this.fbShadow.getDepthTex();
    }

    public int getDebugTexture() {
        return this.fbShadow.getTexture(0);
    }
    @Override
    public void tickUpdate() {
    }
}
