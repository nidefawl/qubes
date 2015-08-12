package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;
import java.util.Arrays;

import nidefawl.game.Main;
import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 2;

    public Vector3f           sun             = new Vector3f(-0.31F, 0.95F, 0.00F);
    public Vector3f           up              = new Vector3f(0, 100, 0);
    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           moonPosition    = new Vector3f(-100, -300, -100);
    public float              sunAngle        = 0.30F;

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
        this.rendered = 0;
        //        sunAngle = 0.45F;
        //        sun = new Vector3f(0.41F, 0.14F, 0.00F);
        //         glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE); // Cull back facing polygons
        //        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glLoadMatrix(Engine.getProjectionMatrix());
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getViewMatrix());
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
        glFogf(GL_FOG_END, Engine.zfar / 7.41F);
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
        prepareRegions(world, fTime);
        if (Main.useShaders) {
            Shaders.terrain.enable();
            Shaders.setUniforms(Shaders.terrain, fTime);
            Shaders.terrain.setProgramUniform1i("texture", 0);
            Shaders.terrain.setProgramUniform1i("normals", 2);
            Shaders.terrain.setProgramUniform1i("noisetex", 3);
            Shaders.terrain.setProgramUniform1i("specular", 5);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, 0);

            Shaders.terrain.enable();
            Shaders.setUniforms(Shaders.terrain, fTime);
            Shaders.terrain.setProgramUniform1i("blockTextures", 0);
            Shaders.terrain.setProgramUniform1i("normals", 2);
            Shaders.terrain.setProgramUniform1i("noisetex", 3);
            Shaders.terrain.setProgramUniform1i("specular", 5);
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
