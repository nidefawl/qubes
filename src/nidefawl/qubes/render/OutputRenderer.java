package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import nidefawl.game.Main;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.TimingHelper;
import org.lwjgl.opengl.*;

public class OutputRenderer {

    public void init() {
    }

    public void render(float fTime) {
        if (Main.DO_TIMING) TimingHelper.start(0);
        boolean enableShaders = true;
        GuiOverlayDebug dbg = Main.instance.debugOverlay;
        if (Main.show) {
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
        }


        if (Main.DO_TIMING) TimingHelper.end(0);
        if (Main.DO_TIMING) TimingHelper.start(1);
        Shader.disable();
        Engine.fbComposite0.bind();
        Engine.fbComposite0.clearFrameBuffer();
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("bind fbo composite1");
        if (enableShaders) {
            Shaders.composite1.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite1");
            Shaders.setUniforms(Shaders.composite1, fTime);
        }
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        for (int i = 0; i < 4; i++) {
            GL.bindTexture(GL_TEXTURE0 + i, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(i));
        }

        Engine.drawFullscreenQuad();
        
        if (Main.DO_TIMING) TimingHelper.end(1);
        if (Main.show) {
            Engine.fbComposite0.unbindCurrentFrameBuffer();
            Shader.disable();
            dbg.preDbgFB(false);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(0, 0, i, Engine.getSceneFB().getTexture(i), "TexUnit " + i);
            }
            dbg.drawDbgTexture(0, 0, 4, TMgr.getNoise(), "TexUnit " + 4);
            dbg.drawDbgTexture(0, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(0, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(0, 1, i, Engine.fbComposite0.getTexture(i), "ColAtt " + i);
            }
            dbg.postDbgFB();
        }
        if (Main.DO_TIMING) TimingHelper.start(17);

        GL.generateTextureMipmap(Engine.fbComposite0.getTexture(0), GL_TEXTURE_2D);
        GL.generateTextureMipmap(Engine.fbComposite0.getTexture(3), GL_TEXTURE_2D);
        
        if (Main.DO_TIMING) TimingHelper.end(17);

        if (Main.DO_TIMING) TimingHelper.start(2);
        
        
        Engine.fbComposite1.bind();
        Engine.fbComposite1.clearFrameBuffer();
        if (enableShaders) {
            Shaders.composite2.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite2");
            Shaders.setUniforms(Shaders.composite2, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        for (int i = 0; i < 4; i++) {
            GL.bindTexture(GL_TEXTURE0 + i, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(i));
        }
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT2);
        Engine.drawFullscreenQuad();
        if (Main.DO_TIMING) TimingHelper.end(2);
        
        if (Main.show) {
            Engine.fbComposite1.unbindCurrentFrameBuffer();
            Shader.disable();

            dbg.preDbgFB(false);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(1, 0, i, Engine.fbComposite0.getTexture(i), "TexUnit " + i);
            }
            dbg.drawDbgTexture(1, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(1, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            dbg.drawDbgTexture(1, 1, 2, Engine.fbComposite1.getTexture(2), "ColAtt " + 2);
            dbg.postDbgFB();
        }

        if (Main.DO_TIMING) TimingHelper.start(18);
        GL.generateTextureMipmap(Engine.fbComposite1.getTexture(2), GL_TEXTURE_2D);
        if (Main.DO_TIMING) TimingHelper.end(18);
        
        if (Main.DO_TIMING) TimingHelper.start(3);

        Engine.fbComposite2.bind();
        Engine.fbComposite2.clearFrameBuffer();
        if (enableShaders) {
            Shaders.composite3.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite3");
            Shaders.setUniforms(Shaders.composite3, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        Engine.drawFullscreenQuad();
        if (Main.DO_TIMING) TimingHelper.end(3);

        Engine.fbComposite2.unbindCurrentFrameBuffer();
        if (Main.show) {
            Shader.disable();
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, Engine.fbComposite0.getTexture(0), "TexUnit " + 0);
            dbg.drawDbgTexture(2, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit " + 1);
            dbg.drawDbgTexture(2, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit " + 2);
            dbg.drawDbgTexture(2, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit " + 3);
            dbg.drawDbgTexture(2, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(2, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            dbg.drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "ColAtt " + 0);
            dbg.drawDbgTexture(3, 0, 0, Engine.fbComposite2.getTexture(0), "TexUnit " + 0);
            dbg.drawDbgTexture(3, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit " + 1);
            dbg.drawDbgTexture(3, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit " + 2);
            dbg.drawDbgTexture(3, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit " + 3);
            dbg.drawDbgTexture(3, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(3, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            //    drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "GL_TEXTURE"+0);
            dbg.postDbgFB();
        }

    }
    public void renderFinal(float fTime) {

        if (Main.DO_TIMING) TimingHelper.start(5);
        boolean enableShaders = true;
        if (enableShaders) {
            Shaders.compositeFinal.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader compositeF");
            Shaders.setUniforms(Shaders.compositeFinal, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.fbComposite2.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));

        Engine.drawFullscreenQuad();
        
        Shader.disable();
        if (Main.DO_TIMING) TimingHelper.end(5);
    }
}
