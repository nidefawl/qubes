package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;
import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vec3;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 2;

    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);

    private ArrayList<Region> firstPass       = new ArrayList<Region>();
    private ArrayList<Region> secondPass      = new ArrayList<Region>();
    
    public int numRegions;
    public BlockPos highlight = null;

    public void init() {
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
    }

    private void drawSkybox() {
        int scale = (int) (Engine.zfar / 1.43F);
        int x = -scale;
        int y = -scale / 16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale / 16;
        int z2 = scale;
        Tess.instance.resetState();
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
        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE, Engine.SHADOW_BUFFER_SIZE);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
//          glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, Textures.blockTextureMap);
        glDisable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_COLOR_MATERIAL);
        glMatrixMode(GL_PROJECTION);
        glLoadMatrix(Engine.getShadowProjectionMatrix());
        glMatrixMode(GL_MODELVIEW);
        glLoadMatrix(Engine.getShadowModelViewMatrix());
        Shaders.shadow.enable();
        Shaders.setUniforms(Shaders.shadow, fTime);
        Shaders.shadow.setProgramUniform1i("blockTextures", 0);
        Shaders.shadow.setProgramUniform1f("shadowAngle", Engine.sunAngle);
        Vector3f camPos = Engine.camera.getPosition();
        glTranslatef(-camPos.x, -camPos.y, -camPos.z);
        Engine.fbShadow.bind();
        Engine.fbShadow.clearFrameBuffer();
        glClear(GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
        glClear(16640);
        renderFirstPass(world, fTime);
        Engine.fbShadow.unbindCurrentFrameBuffer();
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        Shader.disable();
        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
    }
    public void renderWorld(World world, float fTime) {
        
        this.rendered = 0;
        
        glDisable(GL_BLEND);
        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getViewMatrix()); //TODO: GET RID OF, load into shader
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
        if (Main.useShaders) {
            Shaders.sky.enable();
//          Shaders.setUniforms(Shaders.terrain, fTime); //???
        }
        glDepthMask(false);
        drawSkybox();
        glDepthMask(true);
        Shader.disable();
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, (Engine.zfar / 2.3F)*0.75F);
        glFogf(GL_FOG_END, (Engine.zfar / 2.3F));
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);
        if (Main.useShaders) {
            Shaders.terrain.enable();
            Shaders.setUniforms(Shaders.terrain, fTime);
            Shaders.terrain.setProgramUniform1i("blockTextures", 0);
            Shaders.terrain.setProgramUniform1i("normals", 2);
            Shaders.terrain.setProgramUniform1i("noisetex", 3);
            Shaders.terrain.setProgramUniform1i("specular", 5);
            
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
        } else {
//            Engine.enableLighting();
            Shaders.testShader.enable();
            Shaders.testShader.setProgramUniform1i("blockTextures", 0);
            Shaders.testShader.setProgramUniform1i("renderWireFrame", Main.renderWireFrame? 1 : 0);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("test shader");
        }
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        renderFirstPass(world, fTime);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (Main.useShaders) {
            Shaders.waterShader.enable();
            Shaders.setUniforms(Shaders.waterShader, fTime);
            Shaders.waterShader.setProgramUniform1i("texture", 0);
            Shaders.waterShader.setProgramUniform1i("normals", 2);
            Shaders.waterShader.setProgramUniform1i("noisetex", 3);
            Shaders.waterShader.setProgramUniform1i("specular", 5);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("water shader");
        }
        renderSecondPass(world, fTime);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");
        Shader.disable();
//        if (!Main.useShaders) {
//            glDisable(GL_LIGHTING);
//        }
        glDisable(GL_FOG);
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
        glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());

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
            glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            glLoadMatrix(Engine.getModelViewMatrix());
            glDisable(GL_TEXTURE_2D);
            Tess.instance.setColor(-1, 120);
            Tess.instance.setOffset(this.highlight.x, this.highlight.y, this.highlight.z);
            float ext = 1/32F;
            float zero = -ext;
            float one = 1+ext;
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
            
            Tess.instance.draw(GL_QUADS);
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
        glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());
        Shaders.normals.enable();
        Engine.checkGLError("Shaders.normals.enable()");
        glLineWidth(3.0F);
        renderFirstPass(world, fTime);
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
//        Tess.instance.add(v1.x, v1.y, v1.z);
//        Tess.instance.add(v2.x, v2.y, v2.z);
        Tess.instance.draw(GL_LINES);
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glPopAttrib();
        
    }

    private int rendered;

    public float sunAngle2;
    
    public void renderFirstPass(World world, float fTime) {
        glDisable(GL_BLEND);
        int size = firstPass.size();

        for (int i = 0; i < size; i++) {
            Region r = firstPass.get(i);
            glPushMatrix();
            // block index of first chunk in this region of REGION_SIZE * REGION_SIZE chunks
            int xOff = r.rX << (Region.REGION_SIZE_BITS + 4);
            int zOff = r.rZ << (Region.REGION_SIZE_BITS + 4);
            glTranslatef(xOff, 0, zOff);
            r.renderRegion(this, fTime, 0);
            glPopMatrix();
            this.rendered += r.getFacesRendered(0);
        }
    }
    public void renderSecondPass(World world, float fTime) {
        //TODO: sort by distance
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int size = secondPass.size();
//        System.out.println(size);
        for (int i = 0; i < size; i++) {
            Region r = secondPass.get(i);
            glPushMatrix();
            int xOff = r.rX << (Region.REGION_SIZE_BITS + 4);
            int zOff = r.rZ << (Region.REGION_SIZE_BITS + 4);
            glTranslatef(xOff, 0, zOff);
            r.renderRegion(this, fTime, 1);
            glPopMatrix();
            this.rendered += r.getFacesRendered(1);
        }
        glDisable(GL_BLEND);
    }


    public int getNumRendered() {
        return this.rendered;
    }
    public void flushRegions() {
        this.numRegions = 0;
        firstPass.clear();
        secondPass.clear();
    }
    public void putRegion(Region r) {
        this.numRegions++;
        if (r.hasPass(0)) {
            firstPass.add(r);
        }
        if (r.hasPass(1)) {
            secondPass.add(r);
        }
    }
}
