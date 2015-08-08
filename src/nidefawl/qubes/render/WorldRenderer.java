package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

import org.lwjgl.util.vector.Vector3f;

public class WorldRenderer {
    public Vector3f sun = new Vector3f(-0.31F, 0.95F, 0.00F);
    public Vector3f up = new Vector3f(0, 100, 0);
    public Vector3f skyColor = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f fogColor = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f moonPosition = new Vector3f(-100, -300, -100);
    public float sunAngle = 0.30F;

    public void init() {
        skyColor = new Vector3f(0.43F, .69F, 1.F);
        skyColor.scale(0.4F);
        // TODO Auto-generated method stub
        
    }

    private void drawSkybox() {
        int scale = (int) (Engine.zfar/2F);
        int x = -scale;
        int y = -scale/16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale/16;
        int z2 = scale;
        Tess.instance.resetState();
        int rgbai = ((int)(fogColor.x*255))<<16|((int)(fogColor.y*255))<<8|((int)(fogColor.z*255));
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
        rgbai = ((int)(skyColor.x*255))<<16|((int)(skyColor.y*255))<<8|((int)(skyColor.z*255));
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

    public void renderWorld(float fTime) {
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
        glFogf(GL_FOG_END, Engine.zfar/1.41F);
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT );
        glDisable(GL_BLEND);
        {
            Shaders.sky.enable();
            drawSkybox();
            Shader.disable();
        }
        glDisable(GL_FOG);
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());

        float w = 4F;
        float v = 1F;
        int num = 8;
        Tess.instance.resetState();
        Shaders.waterShader2.enable();
        Shaders.setUniforms(Shaders.waterShader2, fTime);
        Shaders.waterShader2.setProgramUniform1i("texture", 0);
        Shaders.waterShader2.setProgramUniform1i("normals", 2);
        Shaders.waterShader2.setProgramUniform1i("noisetex", 3);
        Shaders.waterShader2.setProgramUniform1i("specular", 5);
        Tess.instance.setNormals(0, 1, 0);
        int a = 0;
        for (int x = -num; x <= num; x++)
            for (int z = -num; z <= num; z++) {
                Tess.instance.setOffset(x * w * 2, 0, z * w * 2);
                Tess.instance.setBrightness(0xdd0000);
                Tess.instance.setColor(-1, 255);
                Tess.instance.add(-w, 0, w, 0, v/32F);
                Tess.instance.add(w, 0, w, v, v/32F);
                Tess.instance.add(w, 0, -w, v, 0);
                Tess.instance.add(-w, 0, -w, 0, 0);
            }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Textures.texWater2.getGlid());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Textures.texEmpty);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);
        Tess.instance.draw(GL_QUADS);
        Tess.instance.resetState();
        {
            int x = 10;
            int y = 20;
            int z = 1;
            int tw = 22;
            int th = 22;
            Tess.instance.setNormals(0, 0, 1);
            Tess.instance.setBrightness(0x1f0000);
            Tess.instance.setColor(-1, 255);
            Tess.instance.add(x + tw, y + th, z, 1, 0);
            Tess.instance.add(x, y + th, z, 0, 0);
            Tess.instance.add(x, y, z, 0, 1);
            Tess.instance.add(x + tw, y, z, 1, 1);
        }
        Tess.instance.draw(GL_QUADS);
        Shader.disable();
        //            this.debugOverlay.setMessage(""+Engine.readDepth(0,0));
    }
}
