package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static nidefawl.qubes.GLGame.*;
import nidefawl.game.Main;
import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

import org.lwjgl.opengl.*;

public class OutputRenderer {

    public void init() {
    }

    public void render(float fTime) {
        GuiOverlayDebug dbg = Main.instance.debugOverlay;
        glDisable(GL_LIGHTING);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHT1);
        //      glDisable(GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 771);
        dbg.preDbgFB(true);
        dbg.drawDebug();
        glActiveTexture(GL_TEXTURE0);
        dbg.drawDbgTexture(0, 0, 6, Textures.texWater2.getGlid(), "Water");
        dbg.drawDbgTexture(0, 0, 7, Textures.texEmpty, "Blank");
        dbg.postDbgFB();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_ALWAYS);
        glDepthMask(false);
        glDisable(GL_LIGHTING);
        Tess.instance.dontReset();
        {
            int tw = displayWidth;
            int th = displayHeight;
            float x = 0;
            float y = 0;
            Tess.instance.add(x + tw, y, 0, 1, 1);
            Tess.instance.add(x, y, 0, 0, 1);
            Tess.instance.add(x, y + th, 0, 0, 0);
            Tess.instance.add(x + tw, y + th, 0, 1, 0);
        }

        Shader.disable();

        Engine.fbComposite0.bind();
        Engine.fbComposite0.clearFrameBuffer();
        Engine.checkGLError("bind fbo composite1");
        Shaders.composite1.enable();
        Engine.checkGLError("enable shader composite1");
        Shaders.setUniforms(Shaders.composite1, fTime);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getDepthTex());
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
        for (int i = 0; i < 4; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(i));
        }
        Tess.instance.draw(GL_QUADS);
        Engine.fbComposite0.unbindCurrentFrameBuffer();

        Shader.disable();
        dbg.preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        for (int i = 0; i < 4; i++) {
            dbg.drawDbgTexture(0, 0, i, Engine.fb.getTexture(i), "TexUnit " + i);
        }
        dbg.drawDbgTexture(0, 0, 4, Textures.texNoise, "TexUnit " + 4);
        for (int i = 0; i < 4; i++) {
            dbg.drawDbgTexture(0, 1, i, Engine.fbComposite0.getTexture(i), "ColAtt " + i);
        }
        dbg.postDbgFB();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(0));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        Engine.fbComposite1.bind();
        Engine.fbComposite1.clearFrameBuffer();
        Shaders.composite2.enable();
        Engine.checkGLError("enable shader composite2");
        Shaders.setUniforms(Shaders.composite2, fTime);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
        for (int i = 0; i < 4; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(i));
        }
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT2);
        Tess.instance.draw(GL_QUADS);
        for (int i = 0; i < 6; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        Engine.fbComposite1.unbindCurrentFrameBuffer();

        Shader.disable();
        dbg.preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        for (int i = 0; i < 4; i++) {
            dbg.drawDbgTexture(1, 0, i, Engine.fbComposite0.getTexture(i), "TexUnit " + i);
        }
        dbg.drawDbgTexture(1, 1, 2, Engine.fbComposite1.getTexture(2), "ColAtt " + 2);
        dbg.postDbgFB();

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        Engine.fbComposite2.bind();
        Engine.fbComposite2.clearFrameBuffer();
        Shaders.composite3.enable();
        Engine.checkGLError("enable shader composite3");
        Shaders.setUniforms(Shaders.composite3, fTime);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(0));
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        Tess.instance.draw(GL_QUADS);
        for (int i = 0; i < 6; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        Engine.fbComposite2.unbindCurrentFrameBuffer();

        Shader.disable();
        dbg.preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        dbg.drawDbgTexture(2, 0, 0, Engine.fbComposite0.getTexture(0), "TexUnit " + 0);
        dbg.drawDbgTexture(2, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit " + 1);
        dbg.drawDbgTexture(2, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit " + 2);
        dbg.drawDbgTexture(2, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit " + 3);
        dbg.drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "ColAtt " + 0);
        glActiveTexture(GL_TEXTURE0);
        dbg.drawDbgTexture(3, 0, 0, Engine.fbComposite2.getTexture(0), "TexUnit " + 0);
        dbg.drawDbgTexture(3, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit " + 1);
        dbg.drawDbgTexture(3, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit " + 2);
        dbg.drawDbgTexture(3, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit " + 3);
        //    drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "GL_TEXTURE"+0);
        dbg.postDbgFB();

        glDepthMask(true);
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_ALWAYS);
        glDepthMask(false);
        //    
        Shaders.compositeFinal.enable();
        Engine.checkGLError("enable shader compositeF");
        Shaders.setUniforms(Shaders.compositeFinal, fTime);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, Textures.texNoise);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite2.getTexture(0));
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        //  FloatBuffer buff = BufferUtils.createFloatBuffer(16);
        //  buff.position(0).limit(16);
        //  GL11.glGetFloat(GL11.GL_FOG_COLOR, buff);
        //  System.out.println(String.format(Locale.US, GL11.glGetBoolean(GL11.GL_FOG)+" %.2fF, %.2fF, %.2fF, %.2fF", buff.get(0), buff.get(1), buff.get(2), buff.get(3)));

        Tess.instance.draw(GL_QUADS);
        Tess.instance.resetState();
        Shader.disable();
        for (int i = 0; i < 7; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glActiveTexture(GL_TEXTURE0);
    }

}