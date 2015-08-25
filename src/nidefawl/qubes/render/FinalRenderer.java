package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.GL_RGB16;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.TimingHelper;

public class FinalRenderer extends FinalRendererBase {

    public Shader       shaderFinal;
    private FrameBuffer sceneFB;

    @Override
    public void render(float fTime) {
        if (Main.DO_TIMING) TimingHelper.startSec("DebugOverlay");
        if (Main.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
        }
        if (Main.DO_TIMING) TimingHelper.endSec();
    }

    @Override
    public void renderFinal(float fTime) {
        if (Main.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Main.DO_TIMING) TimingHelper.startSec("DebugOverlay2");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(0, 0, 0, Engine.getSceneFB().getTexture(0), "texColor");
            dbg.drawDbgTexture(0, 0, 1, Engine.getSceneFB().getTexture(1), "texNormals");
            dbg.drawDbgTexture(0, 0, 2, Engine.getSceneFB().getTexture(2), "texMaterial");
            dbg.drawDbgTexture(0, 0, 3, Engine.getSceneFB().getDepthTex(), "texDepth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(0, 0, 4, Engine.fbShadow.getDepthTex(), "texShadow", Shaders.depthBufShader, 0.05F, 256.0F);
            dbg.postDbgFB();
            if (Main.DO_TIMING) TimingHelper.endSec();
        }
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());

        if (Main.DO_TIMING)
            TimingHelper.startSec("enableShader");
        shaderFinal.enable();
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shader final");
        if (Main.DO_TIMING)
            TimingHelper.endStart("setProgramUniform");
        shaderFinal.setProgramUniform1i("texColor", 0);
        shaderFinal.setProgramUniform1i("texNormals", 1);
        shaderFinal.setProgramUniform1i("texMaterial", 2);
        shaderFinal.setProgramUniform1i("texDepth", 3);
        shaderFinal.setProgramUniform1i("texShadow", 4);
        shaderFinal.setProgramUniform1f("near", Engine.znear);
        shaderFinal.setProgramUniform1f("far", Engine.zfar);
        shaderFinal.setProgramUniform1i("shadowMapResolution", Engine.SHADOW_BUFFER_SIZE);
        shaderFinal.setProgramUniform1i("shadowDistance", Engine.SHADOW_ORTHO_DIST);
        
        if (Main.DO_TIMING)
            TimingHelper.endStart("bindTexture");

        //        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
        //        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        //        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());


        if (Main.DO_TIMING)
            TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();

        if (Main.DO_TIMING)
            TimingHelper.endStart("disableShader");
        Shader.disable();
        if (Main.DO_TIMING)
            TimingHelper.endSec();
    }

    @Override
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderFinal = assetMgr.loadShader("shaders/basic/finalstage");
            releaseShaders();
            shaderFinal = new_shaderFinal;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            Main.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        releaseFrameBuffers();
        sceneFB = new FrameBuffer(displayWidth, displayHeight);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        sceneFB.setHasDepthAttachment();
        sceneFB.setup();
        Engine.setSceneFB(sceneFB);
    }

    void releaseShaders() {
        if (shaderFinal != null) {
            shaderFinal.release();
            shaderFinal = null;
        }
    }

    void releaseFrameBuffers() {
        if (sceneFB != null) {
            sceneFB.cleanUp();
            sceneFB = null;
        }
        Engine.setSceneFB(null);
    }

    @Override
    public void release() {
        releaseShaders();
        releaseFrameBuffers();
    }

    @Override
    public void init() {
        initShaders();
    }
}
