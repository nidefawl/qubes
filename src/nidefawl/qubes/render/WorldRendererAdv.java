package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import org.lwjgl.opengl.GL30;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class WorldRendererAdv extends WorldRenderer {
    
    public Shader       waterShader;
    public Shader       sky;
    public Shader       sky2;
    public Shader       terrain;
    public Shader       shadow;

    private void releaseShaders() {
        if (sky != null) {
            sky.release();
            sky = null;
        }
        if (waterShader != null) {
            waterShader.release();
            waterShader = null;
        }
        if (terrain != null) {
            terrain.release();
            terrain = null;
        }
        if (shadow != null) {
            shadow.release();
            shadow = null;
        }
    }
    
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_waterShader = assetMgr.loadShader("shaders/adv/water");
            Shader new_sky = assetMgr.loadShader("shaders/adv/sky");
            Shader new_sky2 = assetMgr.loadShader("shaders/adv/sky");
            Shader new_terrain = assetMgr.loadShader("shaders/adv/terrain");
            Shader new_shadow = assetMgr.loadShader("shaders/adv/shadow");
            releaseShaders();
            waterShader = new_waterShader;
            sky = new_sky;
            sky2 = new_sky2;
            terrain = new_terrain;
            shadow = new_shadow;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            Main.instance.addDebugOnScreen("\0uff3333shader "+e.getName()+" failed to compile");
            System.out.println("shader "+e.getName()+" failed to compile");
            System.out.println(e.getLog());
        }
    }

    public void renderShadowPass(World world, float fTime) {
        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE, Engine.SHADOW_BUFFER_SIZE);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        //          glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, Textures.blockTextureMap);
        glDisable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_COLOR_MATERIAL);
        glMatrixMode(GL_PROJECTION);
        GL.glLoadMatrixf(Engine.getShadowProjectionMatrix());
        glMatrixMode(GL_MODELVIEW);
        GL.glLoadMatrixf(Engine.getShadowModelViewMatrix());
        shadow.enable();
        Shaders.setUniforms(shadow, fTime);
        shadow.setProgramUniform1i("blockTextures", 0);
        shadow.setProgramUniform1f("shadowAngle", Engine.sunAngle);
        Vector3f camPos = Engine.camera.getPosition();
        glTranslatef(-camPos.x, -camPos.y, -camPos.z);
        Engine.fbShadow.bind();
        Engine.fbShadow.clearFrameBuffer();
        Engine.regionRenderer.renderFirstPass(world, fTime);
        Engine.fbShadow.unbindCurrentFrameBuffer();
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        Shader.disable();
        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
    }

    public void renderWorld(World world, float fTime) {

        glDisable(GL_BLEND);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix());//TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getViewMatrix());//TODO: GET RID OF, load into shader
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
        sky.enable();
        //      Shaders.setUniforms(Shaders.terrain, fTime); //???
        glDepthMask(false);
        drawSkybox();
        glDepthMask(true);
        Shader.disable();
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getProjectionMatrix());//TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        GL.glLoadMatrixf(Engine.getModelViewMatrix());
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, (Engine.zfar / 2.3F) * 0.75F);
        glFogf(GL_FOG_END, (Engine.zfar / 2.3F));
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);
        terrain.enable();
        Shaders.setUniforms(terrain, fTime);
        terrain.setProgramUniform1i("blockTextures", 0);
        terrain.setProgramUniform1i("normals", 2);
        terrain.setProgramUniform1i("noisetex", 3);
        terrain.setProgramUniform1i("specular", 5);

        //TODO: use direct state (GL.bindTexture)
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, TMgr.getEmpty());
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, TMgr.getEmptyNormalMap());
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, TMgr.getNoise());
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, TMgr.getEmptySpecularMap());
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("terrain shader");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        Engine.regionRenderer.rendered = 0;
        Engine.regionRenderer.renderFirstPass(world, fTime);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        waterShader.enable();
        Shaders.setUniforms(waterShader, fTime);
        waterShader.setProgramUniform1i("texture", 0);
        waterShader.setProgramUniform1i("normals", 2);
        waterShader.setProgramUniform1i("noisetex", 3);
        waterShader.setProgramUniform1i("specular", 5);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("water shader");
        Engine.regionRenderer.renderSecondPass(world, fTime);
        this.rendered = Engine.regionRenderer.rendered;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");
        Shader.disable();
        //        if (!Main.useShaders) {
        //            glDisable(GL_LIGHTING);
        //        }
        glDisable(GL_FOG);
    }

    public void release() {
        releaseShaders();
    }
    @Override
    public void resize(int displayWidth, int displayHeight) {
        // TODO Auto-generated method stub
        super.resize(displayWidth, displayHeight);
    }
}
