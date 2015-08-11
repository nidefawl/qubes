package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.ArrayList;

import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public class WorldRenderer {

    public static final int   NUM_PASSES      = 2;

    public Vector3f           sun             = new Vector3f(-0.31F, 0.95F, 0.00F);
    public Vector3f           up              = new Vector3f(0, 100, 0);
    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           moonPosition    = new Vector3f(-100, -300, -100);
    public float              sunAngle        = 0.30F;

    private ArrayList<Region> regionsToRender = new ArrayList<Region>();
    private ArrayList<Region> firstPass       = new ArrayList<Region>();
    private ArrayList<Region> secondPass      = new ArrayList<Region>();

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
        glFogf(GL_FOG_END, Engine.zfar / 1.41F);
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT);
        glDisable(GL_BLEND);
        {
            Shaders.sky.enable();
            drawSkybox();
            Shader.disable();
        }
        glDisable(GL_FOG);
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());
        updateTerrain(world, fTime);
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
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Textures.texStone.getGlid());
        renderFirstPass(world, fTime);
        float w = 4F;
        float v = 1F;
        int num = 8;
//        Tess.instance.resetState();
        Shaders.waterShader2.enable();
        Shaders.setUniforms(Shaders.waterShader2, fTime);
        Shaders.waterShader2.setProgramUniform1i("texture", 0);
        Shaders.waterShader2.setProgramUniform1i("normals", 2);
        Shaders.waterShader2.setProgramUniform1i("noisetex", 3);
        Shaders.waterShader2.setProgramUniform1i("specular", 5);
//        Tess.instance.setNormals(0, 1, 0);
//        int a = 0;
//        for (int x = -num; x <= num; x++)
//            for (int z = -num; z <= num; z++) {
//                Tess.instance.setOffset(x * w * 2, 0, z * w * 2);
//                Tess.instance.setBrightness(0xf00000);
//                Tess.instance.setColor(-1, 255);
//                Tess.instance.add(-w, 0, w, 0, v / 32F);
//                Tess.instance.add(w, 0, w, v, v / 32F);
//                Tess.instance.add(w, 0, -w, v, 0);
//                Tess.instance.add(-w, 0, -w, 0, 0);
//            }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Textures.texWater2.getGlid());
        renderSecondPass(world, fTime);
//      Tess.instance.draw(GL_QUADS);
        Shader.disable();
//        Tess.instance.resetState();
//        {
//            int x = 10;
//            int y = 20;
//            int z = 1;
//            int tw = 22;
//            int th = 22;
//            Tess.instance.setNormals(0, 0, 1);
//            Tess.instance.setBrightness(0xc00000);
//            Tess.instance.setColor(-1, 255);
//            Tess.instance.add(x + tw, y + th, z, v, 0);
//            Tess.instance.add(x, y + th, z, 0, 0);
//            Tess.instance.add(x, y, z, 0, v / 32F);
//            Tess.instance.add(x + tw, y, z, v, v / 32F);
//        }
//        Tess.instance.draw(GL_QUADS);
        //            this.debugOverlay.setMessage(""+Engine.readDepth(0,0));
    }

    private int rendered;
    void updateTerrain(World world, float fTime) {
        boolean created = false;
        firstPass.clear();
        secondPass.clear();
        int len = this.regionsToRender.size();
        for (int a = 0; a < len; a++) {
            Region r = this.regionsToRender.get(a);
            if (r == null)
                continue;
            if (r.isEmpty())
                continue;

            if (!r.isRendered()) {
                if (created) {
                    continue;
                }
                if (!Engine.hasFree()) {
                    continue;
                }

                created = true;
                r.makeRegion(this);
            } else if (r.hasBlockData() && world.loader.hasAllNeighBours(r)) {
                //                r.flushBlockData();
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

    public void flush() {
        for (Region r : this.regionsToRender) {
            if (r != null) {
                r.release();
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.regionsToRender.clear();
    }


    public void regionRemoved(Region region) {
        regionsToRender.remove(region);
        region.index = -1;
    }

    public void regionGenerated(int regionX, int regionZ, Region region) {
        region.index = regionsToRender.size();
        regionsToRender.add(region);
    }

    public int getNumRendered() {
        return this.rendered;
    }
}
