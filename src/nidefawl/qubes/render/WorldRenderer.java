package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.Color;
import java.util.HashMap;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.World;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 3;

    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
//    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();

    

    protected int rendered;

    private boolean startup;


    public Shader       testShader;
    public Shader       skyShader;
    public Shader       shadowShader;

    private TesselatorState skybox1;
    private TesselatorState skybox2;


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
        skybox1 = new TesselatorState();
        skybox2 = new TesselatorState();
    }

    public void renderShadowPass(World world, float fTime) {
//        glDisable(GL_CULL_FACE);
//        glCullFace(GL_FRONT);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2.f);
        glDisable(GL_BLEND);

        if (Main.DO_TIMING)
            TimingHelper.startSec("viewport");
        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE/2, Engine.SHADOW_BUFFER_SIZE/2);
        if (Main.DO_TIMING)
            TimingHelper.endStart("bindtex");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        if (Main.DO_TIMING)
            TimingHelper.endStart("enableshader");
        shadowShader.enable();
        shadowShader.setProgramUniform1i("blockTextures", 0);
        shadowShader.setProgramUniform1i("shadowSplit", 0);

        if (Main.DO_TIMING)
            TimingHelper.endStart("bindfb");
        Engine.fbShadow.bind();
        if (Main.DO_TIMING)
            TimingHelper.endStart("clearfb");
        Engine.fbShadow.clearFrameBuffer();
        if (Main.DO_TIMING)
            TimingHelper.endStart("render1");
        Engine.regionRenderer.renderRegions(world, fTime, 2, 1, RegionRenderer.IN_FRUSTUM);
        shadowShader.setProgramUniform1i("shadowSplit", 1);
        
        glPolygonOffset(2.4f, 2.f);

        if (Main.DO_TIMING)
            TimingHelper.endStart("viewport");
        glViewport(Engine.SHADOW_BUFFER_SIZE/2, 0, Engine.SHADOW_BUFFER_SIZE/2, Engine.SHADOW_BUFFER_SIZE/2);
//        Engine.fbShadow2.bind();
//        Engine.fbShadow2.clearFrameBuffer();
        if (Main.DO_TIMING)
            TimingHelper.endStart("render2");
        Engine.regionRenderer.renderRegions(world, fTime, 2, 2, RegionRenderer.IN_FRUSTUM);
        shadowShader.setProgramUniform1i("shadowSplit", 2);
        
        glPolygonOffset(4.4f, 2.f);

        if (Main.DO_TIMING)
            TimingHelper.endStart("viewport");
        glViewport(0, Engine.SHADOW_BUFFER_SIZE/2, Engine.SHADOW_BUFFER_SIZE/2, Engine.SHADOW_BUFFER_SIZE/2);
//        Engine.fbShadow3.bind();
//        Engine.fbShadow3.clearFrameBuffer();
        if (Main.DO_TIMING)
            TimingHelper.endStart("render3");
        Engine.regionRenderer.renderRegions(world, fTime, 2, 3, RegionRenderer.IN_FRUSTUM);
        

        if (Main.DO_TIMING)
            TimingHelper.endStart("unbindFramebuffer");
        FrameBuffer.unbindFramebuffer();
        

        if (Main.DO_TIMING)
            TimingHelper.endStart("disable");
        Shader.disable();

        if (Main.DO_TIMING)
            TimingHelper.endStart("viewport");
        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
        glCullFace(GL_BACK);
        glDisable(GL_POLYGON_OFFSET_FILL);
        if (Main.DO_TIMING)
            TimingHelper.endSec();
    }

    public void renderWorld(World world, float fTime) {

        if (Main.DO_TIMING)
            TimingHelper.startSec("setupView");

        glDisable(GL_BLEND);
        if (Main.DO_TIMING)
            TimingHelper.endStart("Sky");
        glDepthMask(false);
        skyShader.enable();
        skybox1.bindAndDraw(GL_QUAD_STRIP);
        skybox2.bindAndDraw(GL_QUADS);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("skyShader.drawSkybox");
        Shader.disable();
        glDepthMask(true);
        if (Main.DO_TIMING)
            TimingHelper.endStart("setupView2");

        if (Main.DO_TIMING)
            TimingHelper.endStart("testShader");
        testShader.enable();
        testShader.setProgramUniform1i("blockTextures", 0);
        testShader.setProgramUniform1i("renderWireFrame", Main.renderWireFrame ? 1 : 0);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("test shader");
        
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        Engine.regionRenderer.rendered = 0;
        if (Main.DO_TIMING)
            TimingHelper.endStart("renderFirstPass");
        glDisable(GL_BLEND);
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, RegionRenderer.IN_FRUSTUM);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (Main.DO_TIMING)
            TimingHelper.endStart("renderSecondPass");

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, RegionRenderer.IN_FRUSTUM);
        glDisable(GL_BLEND);
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
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, RegionRenderer.ALL);
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
        Tess tesselator = Tess.instance;
//        tesselator.setColorRGBAF(1, 1, 1, 0.2F);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(one, zero, zero);
//        tesselator.add(one, one, zero);
//        tesselator.add(zero, one, zero);
//        tesselator.add(zero, one, one);
//        tesselator.add(one, one, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(zero, zero, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(one, one, one);
//        tesselator.add(one, one, zero);
//        tesselator.add(one, zero, zero);
//        tesselator.add(zero, one, one);
//        tesselator.add(zero, zero, one);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(zero, one, zero);
//        tesselator.add(zero, zero, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(one, zero, zero);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(one, one, one);
//        tesselator.add(zero, one, one);
//        tesselator.add(zero, one, zero);
//        tesselator.add(one, one, zero);
//        tesselator.draw(GL_QUADS, highlightCube);
//        tesselator.resetState();

        

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
        tesselator.setColor(rgbai, 255);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x, y2, z2);
        tesselator.add(x, y, z2);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.draw(GL_QUAD_STRIP, skybox1);
        //      tesselator.draw(GL_TRIANGLE_STRIP);

        rgbai = ((int) (skyColor.x * 255.0F)) << 16 | ((int) (skyColor.y * 255.0F)) << 8 | ((int) (skyColor.z * 255.0F));
        tesselator.setColor(-1, 255);
        tesselator.add(x, y, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x2, y, z);
        tesselator.add(x, y, z);
        tesselator.add(x, y2, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x, y2, z2);
        //    tesselator.draw(GL_TRIANGLES);
        tesselator.draw(GL_QUADS, skybox2);
        
    }

}
