package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.Color;
import java.util.HashMap;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.World;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 2;

    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
//    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();
    
    public BlockPos highlight = null;
    public float sunAngle2;

    protected int rendered;

    private boolean startup;


    public Shader       testShader;
    public Shader       skyShader;
    public Shader       shadowShader;

    private TesselatorState highlightCube;


    private void releaseShaders() {
        if (testShader != null) {
            testShader.release();
            testShader = null;
        }
        if (skyShader != null) {
            skyShader.release();
            skyShader = null;
        }
        if (shadowShader != null) {
            shadowShader.release();
            shadowShader = null;
        }
    }
    
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader terrain = assetMgr.loadShader("shaders/basic/terrain");
            Shader sky = assetMgr.loadShader("shaders/basic/sky");
            Shader shadow = assetMgr.loadShader("shaders/basic/shadow");
            releaseShaders();
            testShader = terrain;
            skyShader = sky;
            shadowShader = shadow;
            startup = false;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            if (startup) {
                System.out.println(e.getLog());
                Main.instance.setException(e);
            } else {
                Main.instance.addDebugOnScreen("\0uff3333shader "+e.getName()+" failed to compile");
                System.out.println("shader "+e.getName()+" failed to compile");
                System.out.println(e.getLog());
            }
        }
    }

    public void init() {
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
        initShaders();
        highlightCube = new TesselatorState();
    }

    protected void drawSkybox() {
        int scale = (int) (Engine.zfar / 1.43F);
        int x = -scale;
        int y = -scale / 16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale / 16;
        int z2 = scale;
        int rgbai = 0;
        rgbai = ((int) (fogColor.x * 255.0F)) << 16 | ((int) (fogColor.y * 255.0F)) << 8 | ((int) (fogColor.z * 255.0F));
//      Shaders.colored.enable();
      Tess.instance.setColor(rgbai, 255);
      Tess.instance.add(x, y2, z);
      Tess.instance.add(x, y, z);
      Tess.instance.add(x2, y2, z);
      Tess.instance.add(x2, y, z);
      Tess.instance.add(x2, y2, z2);
      Tess.instance.add(x2, y, z2);
      Tess.instance.add(x, y2, z2);
      Tess.instance.add(x, y, z2);
      Tess.instance.add(x, y2, z);
      Tess.instance.add(x, y, z);
      Tess.instance.draw(GL_QUAD_STRIP);
//      Tess.instance.draw(GL_TRIANGLE_STRIP);
      
      rgbai = ((int) (skyColor.x * 255.0F)) << 16 | ((int) (skyColor.y * 255.0F)) << 8 | ((int) (skyColor.z * 255.0F));
      Tess.instance.setColor(-1, 255);
      Tess.instance.add(x, y, z2);
      Tess.instance.add(x2, y, z2);
      Tess.instance.add(x2, y, z);
      Tess.instance.add(x, y, z);
      Tess.instance.add(x, y2, z);
      Tess.instance.add(x2, y2, z);
      Tess.instance.add(x2, y2, z2);
      Tess.instance.add(x, y2, z2);
//    Tess.instance.draw(GL_TRIANGLES);
      Tess.instance.draw(GL_QUADS);
        Shader.disable();
    }

    public void renderShadowPass(World world, float fTime) {
//        glEnable(GL_CULL_FACE);
//        glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 4.f);

        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE, Engine.SHADOW_BUFFER_SIZE);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        shadowShader.enable();
        shadowShader.setProgramUniform1i("blockTextures", 0);
        Vector3f camPos = Engine.camera.getPosition();
        glTranslatef(-camPos.x, -camPos.y, -camPos.z);
        Engine.fbShadow.bind();
        Engine.fbShadow.clearFrameBuffer();
        Engine.regionRenderer.renderFirstPass(world, fTime);
        Engine.fbShadow.unbindCurrentFrameBuffer();
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        Shader.disable();
        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
//        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    public void renderWorld(World world, float fTime) {

        if (Main.DO_TIMING)
            TimingHelper.startSec("setupView");

        glDisable(GL_BLEND);
        if (Main.DO_TIMING)
            TimingHelper.endStart("Sky");
        glDepthMask(false);
        skyShader.enable();
        drawSkybox();
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("skyShader.drawSkybox");
        Shader.disable();
        glDepthMask(true);
        Shader.disable();
        if (Main.DO_TIMING)
            TimingHelper.endStart("setupView2");
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (Main.DO_TIMING)
            TimingHelper.endStart("testShader");
        testShader.enable();
        testShader.setProgramUniform1i("blockTextures", 0);
        testShader.setProgramUniform1i("renderWireFrame", Main.renderWireFrame ? 1 : 0);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("test shader");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        Engine.regionRenderer.rendered = 0;
        if (Main.DO_TIMING)
            TimingHelper.endStart("renderFirstPass");
        Engine.regionRenderer.renderFirstPass(world, fTime);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (Main.DO_TIMING)
            TimingHelper.endStart("renderSecondPass");

        Engine.regionRenderer.renderSecondPass(world, fTime);
        this.rendered = Engine.regionRenderer.rendered;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");
        Shader.disable();
        //        if (!Main.useShaders) {
        //            glDisable(GL_LIGHTING);
        //        }
        if (Main.DO_TIMING)
            TimingHelper.endSec();
    }


    public void renderBlockHighlight(World world, float fTime) {
        if (this.highlight != null) {
            glEnable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            Shaders.colored.enable();
            Shaders.colored.setProgramUniform3f("in_offset", this.highlight.x, this.highlight.y, this.highlight.z);
            highlightCube.drawQuads();
            Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);
            Shader.disable();
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
        }
    }
    public void renderNormals(World world, float fTime) {
        glPushAttrib(-1);
        glDisable(GL_BLEND);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
//        glEnable(GL_CULL_FACE);
        Shaders.normals.enable();
        Engine.checkGLError("Shaders.normals.enable()");
        glLineWidth(3.0F);
        Engine.checkGLError("glLineWidth");
        Engine.regionRenderer.setDrawMode(ARBGeometryShader4.GL_LINES_ADJACENCY_ARB);
        Engine.regionRenderer.renderFirstPass(world, fTime);
        Engine.regionRenderer.setDrawMode(GL_QUADS);
        Shader.disable();
        glDisable(GL_TEXTURE_2D);
        glLineWidth(2.0F);

        Shaders.colored.enable();
        Engine.regionRenderer.renderDebug(world, fTime);
        Shader.disable();
        glDisable(GL_BLEND);
        glPopAttrib();
        
    }
    
    public int getNumRendered() {
        return this.rendered;
    }

    public void renderDebugBB(World world, float fTime) {
        if (!this.debugBBs.isEmpty()) {
            glPushAttrib(-1);
            glEnable(GL_BLEND);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            //        glEnable(GL_CULL_FACE);
            glDisable(GL_TEXTURE_2D);
            Shaders.colored.enable();
            for (Integer i : debugBBs.keySet()) {
                AABB bb = debugBBs.get(i);
                int iColor = GameMath.randomI(i*19)%33;
                iColor = Color.getHSBColor(iColor/33F, 0.8F, 1.0F).getRGB();
                
                float fMinX = (float) bb.minX;
                float fMinY = (float) bb.minY;
                float fMinZ = (float) bb.minZ;
                float fMaxX = (float) bb.maxX;
                float fMaxY = (float) bb.maxY;
                float fMaxZ = (float) bb.maxZ;
                
                glLineWidth(2.0F);
                float ext = 1/32F;
                float zero = -ext;
                float one = 1+ext;
                Tess.instance.setColor(iColor, 120);
                Tess.instance.add(fMinX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMinY, fMinZ);
                Tess.instance.add(fMinX, fMaxY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMinZ);
                Tess.instance.draw(GL_LINE_STRIP);
                
                Tess.instance.setColor(iColor, 120);
                Tess.instance.add(fMinX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMaxZ);
                Tess.instance.add(fMaxX, fMinY, fMaxZ);
                Tess.instance.add(fMaxX, fMaxY, fMaxZ);
                Tess.instance.add(fMaxX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMinZ);
                Tess.instance.draw(GL_LINES);
            }
            Shader.disable();
            glPopAttrib();
        }
    }

    public void release() {
        releaseShaders();
    }

    public void resize(int displayWidth, int displayHeight) {

        float ext = 1/32F;
        float zero = -ext;
        float one = 1+ext;
        Tess.instance.setColorRGBAF(1, 1, 1, 0.2F);
        Tess.instance.add(zero, zero, zero);
        Tess.instance.add(one, zero, zero);
        Tess.instance.add(one, one, zero);
        Tess.instance.add(zero, one, zero);
        Tess.instance.add(zero, one, one);
        Tess.instance.add(one, one, one);
        Tess.instance.add(one, zero, one);
        Tess.instance.add(zero, zero, one);
        Tess.instance.add(one, zero, one);
        Tess.instance.add(one, one, one);
        Tess.instance.add(one, one, zero);
        Tess.instance.add(one, zero, zero);
        Tess.instance.add(zero, one, one);
        Tess.instance.add(zero, zero, one);
        Tess.instance.add(zero, zero, zero);
        Tess.instance.add(zero, one, zero);
        Tess.instance.add(zero, zero, one);
        Tess.instance.add(one, zero, one);
        Tess.instance.add(one, zero, zero);
        Tess.instance.add(zero, zero, zero);
        Tess.instance.add(one, one, one);
        Tess.instance.add(zero, one, one);
        Tess.instance.add(zero, one, zero);
        Tess.instance.add(one, one, zero);
        Tess.instance.draw(GL_QUADS, highlightCube);
    }

}
