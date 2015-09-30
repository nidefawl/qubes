package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static nidefawl.qubes.render.WorldRenderer.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.world.World;

public class ShadowRenderer {
    public Shader   shadowShader;
    public Shader   shadowShaderLOD;
    private boolean startup = true;

    //results between those 2 modes are equal
    // the geom shader mode uses geometry shader to instanciate the terrain
    // reducing 3*(terrain_in_furstum slices) draw+bind loops to 1 draw + bind loop for the biggest frustum
    public static final int MULTI_DRAW             = 0; // FASTES, GL 3.x 
    public static final int MULTI_DRAW_TEXUTED     = 1; // as 0 + textures to discard transparent pixels, GL 3.x 
    public static final int GEOM_SHADER            = 2; // GL 4.x ONLY, SLOWEST
    public static final int INSTANCED_DRAW         = 3; // AMD ONLY, SLOW 
    public static final int MAX_SHADOW_RENDER_MODE = 4;
    

    private int renderMode = -1;
    private int SHADOW_BUFFER_SIZE = 1024*4;
    private final float[][] viewports = new float[4][];

    public final boolean[] availableRenderModes = new boolean[MAX_SHADOW_RENDER_MODE];
    public final String[] shaderNames = new String[] {
            "shaders/basic/shadow_multi",
            "shaders/basic/shadow_textured",
            "shaders/basic/shadow_instanced_gs",
            "shaders/basic/shadow_instanced_draw",
    };
    private FrameBuffer fbShadow;
    private FloatBuffer viewports_buf;
    private boolean uploadViewport;

    private void releaseShaders() {
        if (shadowShader != null) {
            shadowShader.release();
            shadowShader = null;
        }
        if (shadowShaderLOD != null) {
            shadowShaderLOD.release();
            shadowShaderLOD = null;
        }
    }

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader shadow = assetMgr.loadShader(shaderNames[renderMode]);
            Shader sshadowShaderLOD = assetMgr.loadShader("shaders/basic/shadow_lod");
            releaseShaders();
            shadowShader = shadow;
            shadowShaderLOD = sshadowShaderLOD;
            shadowShader.enable();
            shadowShader.setProgramUniform1i("blockTextures", 0);
            shadowShaderLOD.enable();
            shadowShaderLOD.setProgramUniform1i("blockTextures", 0);
            Shader.disable();
            this.uploadViewport = true;
        } catch (ShaderCompileError e) {
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
    public void setAvaialbeRenderModes() {
        Arrays.fill(availableRenderModes, false);
        availableRenderModes[MULTI_DRAW] = true;
        availableRenderModes[MULTI_DRAW_TEXUTED] = true;
        if (GL.getCaps().GL_ARB_viewport_array && GL.getCaps().GL_EXT_geometry_shader4) {
            Shader shadow=null;
            try {
                AssetManager assetMgr = AssetManager.getInstance();
                shadow = assetMgr.loadShader("shaders/basic/shadow_instanced_gs");
                availableRenderModes[GEOM_SHADER] = true;
            } catch (ShaderCompileError e) {
                availableRenderModes[GEOM_SHADER] = false;
            } finally {
                if (shadow != null) {
                    shadow.release();
                }
            }
        }
        if (GL.getCaps().GL_ARB_viewport_array && GL.getCaps().GL_AMD_vertex_shader_viewport_index) {
            Shader shadow=null;
            try {
                AssetManager assetMgr = AssetManager.getInstance();
                shadow = assetMgr.loadShader("shaders/basic/shadow_instanced_draw");
                availableRenderModes[INSTANCED_DRAW] = true;
            } catch (ShaderCompileError e) {
                availableRenderModes[INSTANCED_DRAW] = false;
            } finally {
                if (shadow != null) {
                    shadow.release();
                }
            }
        }
    }

    public void setRenderMode(int renderMode) {
        if (availableRenderModes[renderMode]) {
            this.renderMode = renderMode;
        } else {
            this.renderMode = MULTI_DRAW;
        }
    }

    public void init() {
        //        skyColor = new Vector3f(0.43F, .69F, 1.F);
        setAvaialbeRenderModes();
        setRenderMode(Game.instance.settings.shadowDrawMode);
        viewports_buf = BufferUtils.createFloatBuffer(viewports.length*4);
        initShaders();
    }
    public void renderGeomShader(World world, float fTime) {
        Engine.regionRenderer.setDrawMode(ARBGeometryShader4.GL_LINES_ADJACENCY_ARB);
        glDisable(GL_CULL_FACE);
        //        glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(4f, 2.f);
        glDisable(GL_BLEND);

        glViewport(0, 0, SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);

        GL41.glViewportIndexedfv(0, (FloatBuffer) viewports_buf.position(0));
        GL41.glViewportIndexedfv(1, (FloatBuffer) viewports_buf.position(4));
        GL41.glViewportIndexedfv(2, (FloatBuffer) viewports_buf.position(8));
        GL41.glViewportIndexedfv(3, (FloatBuffer) viewports_buf.position(12));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        shadowShader.enable();
        if (uploadViewport) {
            uploadViewport = false;
            shadowShader.setProgramUniform2f("shadowMapSize", SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
            for (int i = 0; i < 4; i++) {
                shadowShader.setProgramUniform4f("viewports["+i+"]", this.viewports[i][0], this.viewports[i][1], this.viewports[i][2], this.viewports[i][3]);
            }
        }

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        glViewport(0, 0, Game.displayWidth, Game.displayHeight);
        //        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        Engine.regionRenderer.setDrawMode(-1);
    }

    public void renderInstancedDraw(World world, float fTime) {
        Engine.regionRenderer.setDrawInstances(3);
        glDisable(GL_CULL_FACE);
        //        glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(4f, 2.f);
        glDisable(GL_BLEND);

        glViewport(0, 0, SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);

        GL41.glViewportIndexedfv(0, (FloatBuffer) viewports_buf.position(0));
        GL41.glViewportIndexedfv(1, (FloatBuffer) viewports_buf.position(4));
        GL41.glViewportIndexedfv(2, (FloatBuffer) viewports_buf.position(8));
        GL41.glViewportIndexedfv(3, (FloatBuffer) viewports_buf.position(12));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        shadowShader.enable();
        if (uploadViewport) {
            uploadViewport = false;
            shadowShader.setProgramUniform2f("shadowMapSize", SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
            for (int i = 0; i < 4; i++) {
                shadowShader.setProgramUniform4f("viewports["+i+"]", this.viewports[i][0], this.viewports[i][1], this.viewports[i][2], this.viewports[i][3]);
            }
        }

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        glViewport(0, 0, Game.displayWidth, Game.displayHeight);
        //        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        Engine.regionRenderer.setDrawInstances(0);
    }
    public void renderMultiPass(World world, float fTime) {
//              glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2.f);
        glDisable(GL_BLEND);
        glViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        final int shadowPass = 2;
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
//      Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 1, Frustum.FRUSTUM_INSIDE);
      Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f, 2.f);

        glViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(1.8f, 2.f);

        glViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);

        FrameBuffer.unbindFramebuffer();

        Shader.disable();

        glViewport(0, 0, Game.displayWidth, Game.displayHeight);
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
        glViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        shadowShader.setProgramUniform1i("blockTextures", 0);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(1.2f, 2.f);

        glViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(2.4f, 2.f);

        glViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);

        FrameBuffer.unbindFramebuffer();

        Shader.disable();

        glViewport(0, 0, Game.displayWidth, Game.displayHeight);
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glDisable(GL_BLEND);
//        glEnable(GL_CULL_FACE);
    }

    public void renderShadowPass(World world, float fTime) {
        if (this.renderMode == GEOM_SHADER) {
            renderGeomShader(world, fTime);
        } else if (this.renderMode == INSTANCED_DRAW) {
            renderInstancedDraw(world, fTime);
        } else if (this.renderMode == MULTI_DRAW_TEXUTED) {
            renderMultiPassTextured(world, fTime);
        } else {
            renderMultiPass(world, fTime);
        }
    }

    public void release() {
        releaseShaders();
    }

    public void resize(int displayWidth, int displayHeight) {
        if (this.fbShadow != null)
            this.fbShadow.cleanUp();
        SHADOW_BUFFER_SIZE = 512*8;
        this.fbShadow = new FrameBuffer(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
        this.fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        this.fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        this.fbShadow.setShadowBuffer();
        this.fbShadow.setup();
        this.viewports[0] = new float[] {0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2};
        this.viewports[1] = new float[] {SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2};
        this.viewports[2] = new float[] {0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2};
        this.viewports[3] = new float[] {SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2};
        viewports_buf.position(0);
        viewports_buf.put(this.viewports[0]);
        viewports_buf.put(this.viewports[1]);
        viewports_buf.put(this.viewports[2]);
        viewports_buf.put(this.viewports[3]);
        viewports_buf.flip();
    }

    public int getDepthTex() {
        return this.fbShadow.getDepthTex();
    }

    public int getDebugTexture() {
        return this.fbShadow.getTexture(0);
    }

    public float getTextureSize() {
        return SHADOW_BUFFER_SIZE;
    }

}
