package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.world.World;

public class ShadowRenderer {
    public Shader   shadowShader;
    private boolean startup;

    //results between those 2 modes are equal
    // the geom shader mode uses geometry shader to instanciate the terrain
    // reducing 3*(terrain_in_furstum slices) draw+bind loops to 1 draw + bind loop for the biggest frustum
    public static final int MULTI_DRAW             = 0; // FASTES, GL 3.x 
    public static final int GEOM_SHADER            = 1; // GL 4.x ONLY, SLOWEST
    public static final int INSTANCED_DRAW         = 2; // AMD ONLY, SLOW 
    public static final int MAX_SHADOW_RENDER_MODE = 3;
    

    private int renderMode = -1;
    private int SHADOW_BUFFER_SIZE = 1024*4;
    private final float[][] viewports = new float[4][];

    public final boolean[] availableRenderModes = new boolean[MAX_SHADOW_RENDER_MODE];
    public final String[] shaderNames = new String[] {
            "shaders/basic/shadow_multi",
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
    }

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader shadow = assetMgr.loadShader(shaderNames[renderMode]);
            releaseShaders();
            shadowShader = shadow;
            this.uploadViewport = true;
        } catch (ShaderCompileError e) {
            if (startup) {
                System.out.println(e.getLog());
                Main.instance.setException(e);
            } else {
                Main.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
                System.out.println("shader " + e.getName() + " failed to compile");
                System.out.println(e.getLog());
            }
        }
    }
    public void setAvaialbeRenderModes() {
        Arrays.fill(availableRenderModes, false);
        availableRenderModes[MULTI_DRAW] = true;
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

    private void setRenderMode(int renderMode) {
        if (availableRenderModes[renderMode]) {
            this.renderMode = renderMode;
        } else {
            this.renderMode = MULTI_DRAW;
        }
    }

    public void init() {
        //        skyColor = new Vector3f(0.43F, .69F, 1.F);
        setAvaialbeRenderModes();
        setRenderMode(MULTI_DRAW);
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
        Engine.regionRenderer.renderRegions(world, fTime, 2, 3, RegionRenderer.IN_FRUSTUM);
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
        //        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        Engine.regionRenderer.setDrawMode(GL_QUADS);
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
        Engine.regionRenderer.renderRegions(world, fTime, 2, 3, RegionRenderer.IN_FRUSTUM);
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
        //        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        Engine.regionRenderer.setDrawInstances(0);
    }
    public void renderMultiPass(World world, float fTime) {
        //      glDisable(GL_CULL_FACE);
        //      glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2.f);
        glDisable(GL_BLEND);
        glViewport(0, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        shadowShader.enable();
        shadowShader.setProgramUniform1i("blockTextures", 0);
        shadowShader.setProgramUniform1i("shadowSplit", 0);

        this.fbShadow.bind();
        this.fbShadow.clearFrameBuffer();
        Engine.regionRenderer.renderRegions(world, fTime, 2, 1, RegionRenderer.IN_FRUSTUM);
        shadowShader.setProgramUniform1i("shadowSplit", 1);

        glPolygonOffset(2.4f, 2.f);

        glViewport(SHADOW_BUFFER_SIZE / 2, 0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);

        Engine.regionRenderer.renderRegions(world, fTime, 2, 2, RegionRenderer.IN_FRUSTUM);
        shadowShader.setProgramUniform1i("shadowSplit", 2);

        glPolygonOffset(4.4f, 2.f);

        glViewport(0, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2, SHADOW_BUFFER_SIZE / 2);
        Engine.regionRenderer.renderRegions(world, fTime, 2, 3, RegionRenderer.IN_FRUSTUM);

        FrameBuffer.unbindFramebuffer();

        Shader.disable();

        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    public void renderShadowPass(World world, float fTime) {
        if (this.renderMode == GEOM_SHADER) {
            renderGeomShader(world, fTime);
        } else if (this.renderMode == INSTANCED_DRAW) {
            renderInstancedDraw(world, fTime);
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
