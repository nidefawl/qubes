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
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();
    
    public BlockPos highlight = null;
    public float sunAngle2;

    protected int rendered;

    private boolean startup;

    
    public Shader       testShader;

    private TesselatorState highlightCube;


    private void releaseShaders() {
        if (testShader != null) {
            testShader.release();
            testShader = null;
        }
    }
    
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader terrain = assetMgr.loadShader("shaders/basic/terrain");
            releaseShaders();
            testShader = terrain;
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
        int rgbai = ((int) (fogColor.x * 255.0F)) << 16 | ((int) (fogColor.y * 255.0F)) << 8 | ((int) (fogColor.z * 255.0F));
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
        rgbai = ((int) (skyColor.x * 255.0F)) << 16 | ((int) (skyColor.y * 255.0F)) << 8 | ((int) (skyColor.z * 255.0F));
        Tess.instance.setColor(rgbai, 255);
        Tess.instance.add(x, y, z2);
        Tess.instance.add(x2, y, z2);
        Tess.instance.add(x2, y, z);
        Tess.instance.add(x, y, z);
        Tess.instance.add(x, y2, z);
        Tess.instance.add(x2, y2, z);
        Tess.instance.add(x2, y2, z2);
        Tess.instance.add(x, y2, z2);
        Tess.instance.draw(GL_QUADS);
    }

    public void renderShadowPass(World world, float fTime) {
//        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE, Engine.SHADOW_BUFFER_SIZE);
//        glActiveTexture(GL_TEXTURE0);
//        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
////          glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, Textures.blockTextureMap);
//        glDisable(GL_FOG);
//        glEnable(GL_TEXTURE_2D);
//        glEnable(GL_COLOR_MATERIAL);
//        glMatrixMode(GL_PROJECTION);
//        glLoadMatrix(Engine.getShadowProjectionMatrix());
//        glMatrixMode(GL_MODELVIEW);
//        glLoadMatrix(Engine.getShadowModelViewMatrix());
//        shadow.enable();
//        ShadersAdv.setUniforms(shadow, fTime);
//        shadow.setProgramUniform1i("blockTextures", 0);
//        shadow.setProgramUniform1f("shadowAngle", Engine.sunAngle);
//        Vector3f camPos = Engine.camera.getPosition();
//        glTranslatef(-camPos.x, -camPos.y, -camPos.z);
//        Engine.fbShadow.bind();
//        Engine.fbShadow.clearFrameBuffer();
//        Engine.regionRenderer.renderFirstPass(world, fTime);
//        Engine.fbShadow.unbindCurrentFrameBuffer();
//        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
//        Shader.disable();
//        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
    }
    public void renderWorld(World world, float fTime) {

        if (Main.DO_TIMING) TimingHelper.startSec("setupView");
        
        glDisable(GL_BLEND);
        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getViewMatrix()); //TODO: GET RID OF, load into shader
        if (Main.DO_TIMING) TimingHelper.endStart("preSky");
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        Engine.setFog(fogColor, 1);
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, 0);
        glFogf(GL_FOG_END, Engine.zfar / 2.3F);
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);
        if (Main.DO_TIMING) TimingHelper.endStart("Sky");
//        if (!Main.useBasicShaders) {
//            sky.enable();
////          Shaders.setUniforms(Shaders.terrain, fTime); //???
//        }
        glDepthMask(false);
        drawSkybox();
        glDepthMask(true);
        Shader.disable();
        if (Main.DO_TIMING) TimingHelper.endStart("setupView2");
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getModelViewMatrix());
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, (Engine.zfar / 2.3F)*0.85F);
        glFogf(GL_FOG_END, (Engine.zfar / 2.3F));
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);

        if (Main.DO_TIMING) TimingHelper.endStart("testShader");
//      Engine.enableLighting();
      testShader.enable();
      testShader.setProgramUniform1i("blockTextures", 0);
      testShader.setProgramUniform1i("renderWireFrame", Main.renderWireFrame? 1 : 0);
      if (Main.GL_ERROR_CHECKS)
          Engine.checkGLError("test shader");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        Engine.regionRenderer.rendered = 0;
        if (Main.DO_TIMING) TimingHelper.endStart("renderFirstPass");
        Engine.regionRenderer.renderFirstPass(world, fTime);
        if (Main.DO_TIMING) TimingHelper.endStart("renderSecondPass");
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
//        if (!Main.useBasicShaders) {
//            waterShader.enable();
//            ShadersAdv.setUniforms(waterShader, fTime);
//            waterShader.setProgramUniform1i("texture", 0);
//            waterShader.setProgramUniform1i("normals", 2);
//            waterShader.setProgramUniform1i("noisetex", 3);
//            waterShader.setProgramUniform1i("specular", 5);
//            if (Main.GL_ERROR_CHECKS)
//                Engine.checkGLError("water shader");
//        }
        Engine.regionRenderer.renderSecondPass(world, fTime);
        this.rendered = Engine.regionRenderer.rendered;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");
        Shader.disable();
//        if (!Main.useShaders) {
//            glDisable(GL_LIGHTING);
//        }
        glDisable(GL_FOG);
        if (Main.DO_TIMING) TimingHelper.endSec();
    }

    public void renderViewDir(World world, float fTime) {
        glPushAttrib(-1);
        glDisable(GL_BLEND);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_CULL_FACE);
//        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getModelViewMatrix());

        glDisable(GL_TEXTURE_2D);
        glLineWidth(3.0F);
        Vec3 v1 = Engine.vOrigin;
        Vec3 v2 = Engine.vTarget;
        Tess.instance.setColor(-1, 255);
        Tess.instance.add(v1.x, v1.y, v1.z);
        Tess.instance.add(v2.x, v2.y, v2.z);
        Tess.instance.draw(GL_LINES);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glPopAttrib();
        
    }

    public void renderBlockHighlight(World world, float fTime) {
        if (this.highlight != null) {
            glPushAttrib(-1);
            glEnable(GL_BLEND);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_ALPHA_TEST);
            glDisable(GL_CULL_FACE);
            //        glEnable(GL_CULL_FACE);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            GL.glLoadMatrixf(Engine.getModelViewMatrix());
            glDisable(GL_TEXTURE_2D);
            Shaders.colored.enable();
            glTranslatef(this.highlight.x, this.highlight.y, this.highlight.z);
            highlightCube.drawQuads();
            Shader.disable();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
            glPopAttrib();
        }
    }
    public void renderNormals(World world, float fTime) {
        glPushAttrib(-1);
        glDisable(GL_BLEND);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_ALPHA_TEST);
        glDisable(GL_CULL_FACE);
//        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getModelViewMatrix());
        Shaders.normals.enable();
        Engine.checkGLError("Shaders.normals.enable()");
        glLineWidth(3.0F);
        Engine.checkGLError("glLineWidth");
        Engine.regionRenderer.setDrawMode(ARBGeometryShader4.GL_LINES_ADJACENCY_ARB);
        Engine.regionRenderer.renderFirstPass(world, fTime);
        Engine.regionRenderer.setDrawMode(GL_QUADS);
        Shader.disable();
//        int y = 130;
//        int x = 0;
//        int z = 0;
//        int w = 2;
//        glDisable(GL_TEXTURE_2D);
//        Tess.instance.resetState();
//        Tess.instance.setColor(-1, 255);
//        Tess.instance.add(x,y,z+w);
//        Tess.instance.add(x+w,y,z+w);
//        Tess.instance.add(x+w,y,z);
//        Tess.instance.add(x,y,z);
//        Tess.instance.draw(GL_QUADS);
        glDisable(GL_TEXTURE_2D);
        Vec3 v1 = Engine.vOrigin;
        Vec3 v2 = Engine.vTarget;
//        Vector3f cam = Engine.camera.getPosition();
        glLineWidth(2.0F);
        Tess.instance.setColor(-1, 255);
        Tess.instance.add(0,0,0);
        Tess.instance.add(0,1000,0);
//      Tess.instance.add(v1.x, v1.y, v1.z);
//      Tess.instance.add(v2.x, v2.y, v2.z);
        Tess.instance.draw(GL_LINES);
        Engine.regionRenderer.renderDebug(world, fTime);
        glDisable(GL_BLEND);
        glEnable(GL_ALPHA_TEST);
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
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
            glDisable(GL_ALPHA_TEST);
            glDisable(GL_CULL_FACE);
            //        glEnable(GL_CULL_FACE);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            GL.glLoadMatrixf(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            GL.glLoadMatrixf(Engine.getModelViewMatrix());
            glDisable(GL_TEXTURE_2D);
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
                
                Tess.instance.setColor(iColor, 120);
                float ext = 1/32F;
                float zero = -ext;
                float one = 1+ext;
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
                glLineWidth(2.0F);
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
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
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
        // TODO Auto-generated method stub
        
    }

}
