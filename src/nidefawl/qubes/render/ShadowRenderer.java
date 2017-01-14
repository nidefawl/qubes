package nidefawl.qubes.render;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;

public class ShadowRenderer extends AbstractRenderer {
    public Shader   shadowShader;
    private boolean startup = true;

    //results between those 2 modes are equal
    // the geom shader mode uses geometry shader to instanciate the terrain
    // reducing 3*(terrain_in_furstum slices) draw+bind loops to 1 draw + bind loop for the biggest frustum
    public static final int MULTI_DRAW             = 0; // FASTES, GL 3.x 
    public static final int MULTI_DRAW_TEXUTED     = 1; // as 0 + textures to discard transparent pixels, GL 3.x 
    public static final int MAX_SHADOW_RENDER_MODE     = 2; 
    

    private int renderMode = -1;
    private int SHADOW_BUFFER_SIZE = 1024*4;

    public final String[] shaderNames = new String[] {
            "shadow/shadow_multi",
            "shadow/shadow_textured",
    };
    private FrameBuffer fbShadow;


    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            pushCurrentShaders();
            Shader shadow = assetMgr.loadShader(this, shaderNames[renderMode], new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("MATRIX".equals(define)) {
                        return "#define MATRIX in_matrix_shadow.shadow_split_mvp[shadowSplit] * model_matrix";
                    }
                    return null;
                }
            });
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


    public void init() {
        this.renderMode = Game.instance.settings.shadowDrawMode>MULTI_DRAW_TEXUTED||Game.instance.settings.shadowDrawMode<0?MULTI_DRAW:Game.instance.settings.shadowDrawMode;
        initShaders();
    }
    
    public void renderMultiPass(World world, float fTime) {
//              glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2.f);
        Engine.setViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        final int shadowPass = 2;
//      Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
//    Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 1, Frustum.FRUSTUM_INSIDE);

        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        Engine.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 0); //TODO: FRUSTUM CULLING

        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f, 2.f);

        Engine.setViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        Engine.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 1); //TODO: FRUSTUM CULLING

        shadowShader.enable();
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(1.4f, 2.f);

        Engine.setViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        Engine.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, shadowShader, 2); //TODO: FRUSTUM CULLING
//        shadowShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);

//        FrameBuffer.unbindFramebuffer();

//        Shader.disable();

        Engine.setDefaultViewport();
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
//        glEnable(GL_CULL_FACE);
    }

    public void renderMultiPassTextured(World world, float fTime) {
//              glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2.f);
        glEnable(GL_BLEND);
        Engine.setViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        shadowShader.setProgramUniform1i("blockTextures", 0);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
//        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f, 2.f);

        Engine.setViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(2.4f, 2.f);

        Engine.setViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);

//        FrameBuffer.unbindFramebuffer();

//        Shader.disable();

        Engine.setDefaultViewport();
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
//        glEnable(GL_CULL_FACE);
    }

    public void renderShadowPass(World world, float fTime) {
        if (this.renderMode == MULTI_DRAW_TEXUTED) {
            renderMultiPassTextured(world, fTime);
        } else {
            renderMultiPass(world, fTime);
        }
    }

    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        SHADOW_BUFFER_SIZE = getTextureSize();
        this.fbShadow = new FrameBuffer(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
        this.fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        this.fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        this.fbShadow.setShadowBuffer();
        this.fbShadow.setup(this);
    }

    public int getDepthTex() {
        return this.fbShadow.getDepthTex();
    }

    public int getDebugTexture() {
        return this.fbShadow.getTexture(0);
    }

    public int getTextureSize() {
        return GameBase.VR_SUPPORT?1024*1:1024*4;
    }

}
