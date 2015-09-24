package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.awt.Color;
import java.util.HashMap;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 3;

    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
//    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();

    

    public int rendered;

    private boolean startup = true;


    public int                  texWaterNormals;
    public Shader       terrainShader;
    public Shader       skyShader;
    public Shader       waterShader;
    
    private TesselatorState skybox1;
    private TesselatorState skybox2;


    private void releaseShaders() {
        if (terrainShader != null) {
            terrainShader.release();
            terrainShader = null;
        }
        if (waterShader != null) {
            waterShader.release();
            waterShader = null;
        }
        if (skyShader != null) {
            skyShader.release();
            skyShader = null;
        }
    }
    
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_waterShader = assetMgr.loadShader("shaders/basic/water");
            Shader terrain = assetMgr.loadShader("shaders/basic/terrain");
            Shader sky = assetMgr.loadShader("shaders/basic/sky");
            releaseShaders();
            terrainShader = terrain;
            skyShader = sky;
            waterShader = new_waterShader;
            
            terrainShader.enable();
            terrainShader.setProgramUniform1i("blockTextures", 0);
            terrainShader.setProgramUniform1i("noisetex", 1);
            
            waterShader.enable();
            waterShader.setProgramUniform1i("blockTextures", 0);
            waterShader.setProgramUniform1i("waterNormals", 1);
            Shader.disable();
            startup = false;
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

    public void init() {
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
        initShaders();
        skybox1 = new TesselatorState();
        skybox2 = new TesselatorState();
        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/water/normals.png");
        texWaterNormals = TextureManager.getInstance().makeNewTexture(tex, true, true, 10);
    }

    public void renderWorld(World world, float fTime) {

        if (Game.DO_TIMING)
            TimingHelper.startSec("setupView");

        glDisable(GL_BLEND);
        if (Game.DO_TIMING)
            TimingHelper.endStart("Sky");
        glDepthMask(false);
        skyShader.enable();
        skybox1.bindAndDraw(GL_QUAD_STRIP);
        skybox2.bindAndDraw(GL_QUADS);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("skyShader.drawSkybox");
        Shader.disable();
        glDepthMask(true);
        if (Game.DO_TIMING)
            TimingHelper.endStart("setupView2");

        if (Game.DO_TIMING)
            TimingHelper.endStart("testShader");
        
        terrainShader.enable();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("terrain shader");
        
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        
        Engine.regionRenderer.rendered = 0;
        if (Game.DO_TIMING)
            TimingHelper.endStart("renderFirstPass");
//        glDisable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
        rendered = Engine.regionRenderer.rendered;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (Game.DO_TIMING)
            TimingHelper.endStart("renderSecondPass");

        waterShader.enable();
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.texWaterNormals);
        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
        glDisable(GL_BLEND);
        this.rendered = Engine.regionRenderer.rendered;
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");
        
        Shader.disable();

        if (Game.DO_TIMING)
            TimingHelper.endSec();
    }

    public void renderNormals(World world, float fTime) {
//        Shaders.normals.enable();
//        glLineWidth(3.0F);
//        Engine.checkGLError("glLineWidth");
////        Engine.regionRenderer.setDrawMode(ARBGeometryShader4.GL_T);
//        Engine.regionRenderer.setDrawMode(-1);
//        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
//        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
//        Engine.regionRenderer.setDrawMode(-1);
////        glLineWidth(2.0F);
//
////        Shaders.colored.enable();
////        Engine.regionRenderer.renderDebug(world, fTime);
//        Shader.disable();
        
    }

    public void renderTerrainWireFrame(World world, float fTime) {
        Shaders.wireframe.enable();
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 1, 0.2f, 1);
        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
        Shader.disable();
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
