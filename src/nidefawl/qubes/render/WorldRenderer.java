package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;
import java.util.Arrays;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.Textures;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 2;

    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);

    private ArrayList<Region> firstPass       = new ArrayList<Region>();
    private ArrayList<Region> secondPass      = new ArrayList<Region>();
    Region[] renderRegions = new Region[RegionLoader.MAX_REGIONS];
    public int numRegions;

    public void init() {
        skyColor = new Vector3f(0.43F, .69F, 1.F);
        skyColor.scale(0.4F);
    }

    private void drawSkybox() {
        int scale = (int) (Engine.zfar / 2F);
        int x = -scale;
        int y = -scale / 16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale / 16;
        int z2 = scale;
        Tess.instance.resetState();
        int rgbai = ((int) (fogColor.x * 255)) << 16 | ((int) (fogColor.y * 255)) << 8 | ((int) (fogColor.z * 255));
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
        rgbai = ((int) (skyColor.x * 255)) << 16 | ((int) (skyColor.y * 255)) << 8 | ((int) (skyColor.z * 255));
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

    public void renderWorld(World world, float fTime) {
        prepareRegions(world, fTime);
        
        this.rendered = 0;
//        Engine.suncamera.set(x, y, z, yaw, pitch);
        //        sun = new Vector3f(0.41F, 0.14F, 0.00F);
        //         glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE); // Cull back facing polygons
        //        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
//        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, Textures.blockTextureMap);
        Engine.fbShadow.bind();
        Engine.fbShadow.clearFrameBuffer();
        glClear(GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glClear(16640);
        glDisable(GL_FOG);
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_COLOR_MATERIAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, Engine.SHADOW_BUFFER_SIZE, Engine.SHADOW_BUFFER_SIZE);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();//Required?
        glLoadMatrix(Engine.getShadowProjectionMatrix());
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getShadowModelViewMatrix());
        if (Main.useShaders) {
            Shaders.shadow.enable();
            Shaders.setUniforms(Shaders.shadow, fTime);
            Shaders.shadow.setProgramUniform1i("blockTextures", 0);
            Shaders.shadow.setProgramUniform1f("shadowAngle", Engine.sunAngle);
        }
        
        Vector3f camPos = Engine.camera.getPosition();
        glTranslatef(-camPos.x, -camPos.y, -camPos.z);
        renderFirstPass(world, fTime);
        Shader.disable();
        Engine.fbShadow.unbindCurrentFrameBuffer();
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        glViewport(0, 0, Main.displayWidth, Main.displayHeight);
        Engine.getSceneFB().bind();
        Engine.getSceneFB().clearFrameBuffer();
        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glLoadMatrix(Engine.getProjectionMatrix()); //TODO: GET RID OF, load into shader
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getViewMatrix()); //TODO: GET RID OF, load into shader
        glDisable(GL_TEXTURE_2D);
        glDepthMask(false);
        glDisable(GL_ALPHA_TEST);
        //        Vector3f fogColor2 = new Vector3f(0.7F, 0,0);
        //        fogColor2.scale(0.4F);
        Engine.setFog(fogColor, 1);
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, 0);
        glFogf(GL_FOG_END, Engine.zfar / 1.41F);
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);
        glDisable(GL_BLEND);
        if (Main.useShaders) {
            Shaders.sky.enable();
//          Shaders.setUniforms(Shaders.terrain, fTime); //???
        }
        drawSkybox();
        Shader.disable();
        glDisable(GL_FOG);
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());
        if (Main.useShaders) {
            Shaders.terrain.enable();
            Shaders.setUniforms(Shaders.terrain, fTime);
            Shaders.terrain.setProgramUniform1i("blockTextures", 0);
            Shaders.terrain.setProgramUniform1i("normals", 2);
            Shaders.terrain.setProgramUniform1i("noisetex", 3);
            Shaders.terrain.setProgramUniform1i("specular", 5);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, Textures.texEmptyNormal);
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);

        } else {
            Engine.enableLighting();
            Shaders.testShader.enable();
            Shaders.testShader.setProgramUniform1i("blockTextures", 0);
        }
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, Textures.blockTextureMap);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("setProgramUniform1i");
        renderFirstPass(world, fTime);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        Shader.disable();
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (Main.useShaders) {
            Shaders.waterShader2.enable();
            Shaders.setUniforms(Shaders.waterShader2, fTime);
            Shaders.waterShader2.setProgramUniform1i("texture", 0);
            Shaders.waterShader2.setProgramUniform1i("normals", 2);
            Shaders.waterShader2.setProgramUniform1i("noisetex", 3);
            Shaders.waterShader2.setProgramUniform1i("specular", 5);
        }
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Textures.texWater2.getGlid());
        renderSecondPass(world, fTime);
        Shader.disable();
        if (!Main.useShaders) {
            glDisable(GL_LIGHTING);
        }
    }

    private int rendered;

    public float sunAngle2;
    void prepareRegions(World world, float fTime) {
        int loaded = 0;
//      if (follow && this.regions.size() >= MAX_REGIONS - 20   ) {
//          Iterator<Long> it = this.regionLoadReqMap.iterator();
//          for (; it.hasNext();) {
//              Long l = it.next();
//              int rx = GameMath.lhToX(l);
//              int rz = GameMath.lhToZ(l);
//              int dX = Math.abs(x - rx);
//              int dZ = Math.abs(z - rz);
//              if (dX > LOAD_DIST || dZ > LOAD_DIST) {
//                  synchronized (this.regions) {
//                      it.remove();
//                      this.loadQueue.remove(l);
//                      Region r = this.regions.remove(l);
//                      if (r != null) {
//                          r.release();
//                          Engine.worldRenderer.regionRemoved(r);
//                      }
//                  }
//              }
//          }
//      }
  
        boolean created = false;
        firstPass.clear();
        secondPass.clear();
        for (int a = 0; a < this.numRegions; a++) {
            Region r = this.renderRegions[a];
            if (r == null)
                continue;
            if (r.isEmpty())
                continue;
            if (r.renderState < Region.RENDER_STATE_MESHED)
                continue;
            if (r.renderState == Region.RENDER_STATE_MESHED) {
                if (created) {
                    continue;
                }
                if (!Engine.hasFree()) {
                    continue;
                }

                created = true;
                r.makeRegion(this);
                r.renderState = Region.RENDER_STATE_COMPILED;
            }
            if (r.hasPass(0)) {
                firstPass.add(r);
            }
            if (r.hasPass(1)) {
                secondPass.add(r);
            }
        }
    }
    public void renderFirstPass(World world, float fTime) {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
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
        GL11.glEnable(GL11.GL_BLEND);
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
        GL11.glDisable(GL11.GL_BLEND);
    }


    public int getNumRendered() {
        return this.rendered;
    }
    public void flushRegions() {
        this.numRegions = 0;
        Arrays.fill(this.renderRegions, null);
    }
    public void putRegion(Region r) {
        this.renderRegions[this.numRegions++] = r;
    }
}
