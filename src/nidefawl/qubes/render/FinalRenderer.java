package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.GL11;

import nidefawl.game.GL;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.TimingHelper;

public class FinalRenderer {

    public Shader shaderFinal;
    public Shader shaderBlur;
    public Shader shaderDeferred;
    public Shader shaderThreshold;

    private FrameBuffer sceneFB;
    private FrameBuffer deferred;
    private FrameBuffer blur1;
    private FrameBuffer blur2;
    private int         blurTexture;

    public void renderDeferred(float fTime) {

        if (Main.DO_TIMING)
            TimingHelper.startSec("bindFramebuffer");
        deferred.bind();
        if (Main.DO_TIMING)
            TimingHelper.endStart("clearFrameBuffer");
        deferred.clearFrameBuffer();

        if (Main.DO_TIMING)
            TimingHelper.endStart("enableShader");
        shaderDeferred.enable();
        shaderDeferred.setProgramUniform1f("near", Engine.znear);
        shaderDeferred.setProgramUniform1f("far", Engine.zfar);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderDeferred");

        if (Main.DO_TIMING)
            TimingHelper.endStart("bindTextures");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getNoise());

        if (Main.DO_TIMING)
            TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();

        if (Main.DO_TIMING)
            TimingHelper.endSec();

        if (Main.show) {
            Shader.disable();
            FrameBuffer.unbindFramebuffer();
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Main.DO_TIMING)
                TimingHelper.startSec("DebugOverlayDef");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(0, 0, 0, Engine.getSceneFB().getTexture(0), "texColor");
            dbg.drawDbgTexture(0, 0, 1, Engine.getSceneFB().getTexture(1), "texNormals");
            dbg.drawDbgTexture(0, 0, 2, Engine.getSceneFB().getTexture(2), "texMaterial");
            dbg.drawDbgTexture(0, 0, 3, Engine.getSceneFB().getDepthTex(), "texDepth");
            dbg.drawDbgTexture(0, 0, 4, Engine.fbShadow.getDepthTex(), "texShadow");
            dbg.drawDbgTexture(0, 0, 5, TMgr.getNoise(), "noiseTex");
            dbg.drawDbgTexture(0, 1, 0, this.deferred.getTexture(0), "DeferredOut");
            dbg.postDbgFB();
            if (Main.DO_TIMING)
                TimingHelper.endSec();
        }

    }
    final int[][] kawaseKernelSizePasses = new int[] [] {
        {0,0},
        {0, 1, 1},
        {0, 1, 2, 2, 3},
        {0, 1, 2, 3, 4, 4, 5},
        {0, 1, 2, 3, 4, 5, 7, 8, 9, 10},
    };
    public void renderBlur(float fTime) {
        int kawaseKernSizeSetting = 4;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        
        int input = this.deferred.getTexture(0);
        FrameBuffer buffer = blur1;

        if (Main.DO_TIMING) TimingHelper.startSec("enableShader");
        shaderBlur.enable();
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shaderBlur");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        shaderThreshold.enable();
        if (Main.DO_TIMING) TimingHelper.endStart("bindFramebuffer");
        buffer.bind();
        if (Main.DO_TIMING) TimingHelper.endStart("clearFrameBuffer");
        buffer.clearFrameBuffer();
        if (Main.DO_TIMING)  TimingHelper.endStart("bindTexture");
        float w, h;
        w = deferred.getWidth();
        h = deferred.getHeight();
//        if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
            GL11.glViewport(0, 0, buffer.getWidth(), buffer.getHeight());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        if (Main.DO_TIMING) TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();
        input = buffer.getTexture(0);
        buffer = buffer == blur1 ? blur2 : blur1;
        shaderBlur.enable();
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            if (Main.DO_TIMING) TimingHelper.endStart("setuniforms");
            shaderBlur.setProgramUniform3f("blurPassProp", 1.0F/w, 1.0F/h, kawaseKernPasses[p]);
            if (Main.DO_TIMING) TimingHelper.endStart("bindFramebuffer");
            buffer.bind();
            if (Main.DO_TIMING) TimingHelper.endStart("clearFrameBuffer");
            buffer.clearFrameBuffer();
            if (Main.DO_TIMING)  TimingHelper.endStart("bindTexture");
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
            if (Main.DO_TIMING) TimingHelper.endStart("drawFullscreenQuad");
            Engine.drawFullscreenQuad();
            


            if (Main.show) {
//                if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
                    GL11.glViewport(0, 0, deferred.getWidth(), deferred.getHeight());
                FrameBuffer.unbindFramebuffer();
                Shader.disable();
                GuiOverlayDebug dbg = Main.instance.debugOverlay;
                if (Main.DO_TIMING)
                    TimingHelper.startSec("DebugOverlayBlur");
                dbg.preDbgFB(false);
                dbg.drawDbgTexture(1, 0, p+1, input, "pass"+p+" in");
                dbg.drawDbgTexture(1, 1, p+1, buffer.getTexture(0), "pass"+p+" out");
                dbg.postDbgFB();
                if (Main.DO_TIMING)
                    TimingHelper.endSec();
                shaderBlur.enable();
//                if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
                    GL11.glViewport(0, 0, buffer.getWidth(), buffer.getHeight());
            }
            input = buffer.getTexture(0);
            buffer = buffer == blur1 ? blur2 : blur1;
//            w = buffer.getWidth();
//            h = buffer.getHeight();
        }

        FrameBuffer.unbindFramebuffer();
        Shader.disable();
        if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
            GL11.glViewport(0, 0, deferred.getWidth(), deferred.getHeight());
        if (Main.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Main.DO_TIMING)
                TimingHelper.startSec("DebugOverlayBlur");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(1, 0, 0, deferred.getTexture(0), "texColor");
            dbg.drawDbgTexture(1, 1, 0, input, "blured");
            dbg.postDbgFB();
            if (Main.DO_TIMING)
                TimingHelper.endSec();
        }
        this.blurTexture = input;
        if (Main.DO_TIMING) TimingHelper.endSec();
    }


    public void render(float fTime) {

        if (Main.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
        }
        if (Main.DO_TIMING)
            TimingHelper.startSec("Deferred");
        renderDeferred(fTime);
        if (Main.DO_TIMING)
            TimingHelper.endStart("Blur");
        renderBlur(fTime);
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        FrameBuffer.unbindFramebuffer();

    }


    public void renderFinal(float fTime) {
        if (Main.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Main.DO_TIMING)
                TimingHelper.startSec("DebugOverlayFinal");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, deferred.getTexture(0), "texColor");
            dbg.drawDbgTexture(2, 0, 1, blurTexture, "texBlur");
            dbg.postDbgFB();
            if (Main.DO_TIMING)
                TimingHelper.endSec();
        }

        if (Main.DO_TIMING)
            TimingHelper.endStart("enableShader");
        shaderFinal.enable();
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        if (Main.DO_TIMING)
            TimingHelper.endStart("bindTexture");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, deferred.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);

        if (Main.DO_TIMING)
            TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();

        if (Main.DO_TIMING)
            TimingHelper.endStart("disableShader");
        Shader.disable();

    }


    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderDef = assetMgr.loadShader("shaders/basic/deferred");
            Shader new_shaderBlur = assetMgr.loadShader("shaders/basic/blur_kawase");
            Shader new_shaderFinal = assetMgr.loadShader("shaders/basic/finalstage");
            Shader new_shaderThresh = assetMgr.loadShader("shaders/basic/thresholdfilter");
            releaseShaders();
            shaderFinal = new_shaderFinal;
            shaderBlur = new_shaderBlur;
            shaderDeferred = new_shaderDef;
            shaderThreshold = new_shaderThresh;
            shaderFinal.enable();
            shaderFinal.setProgramUniform1i("texColor", 0);
            shaderFinal.setProgramUniform1i("texBlur", 1);
            shaderBlur.enable();
            shaderBlur.setProgramUniform1i("texColor", 0);
            shaderThreshold.enable();
            shaderThreshold.setProgramUniform1i("texColor", 0);
            shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("noisetex", 5);
            Shader.disable();
        } catch (ShaderCompileError e) {
            Main.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
    }


    public void resize(int displayWidth, int displayHeight) {
        releaseFrameBuffers();
        sceneFB = new FrameBuffer(displayWidth, displayHeight);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        sceneFB.setHasDepthAttachment();
        sceneFB.setup();
        Engine.setSceneFB(sceneFB);
        int blurDownSample = 4;
        int blurW = displayWidth/blurDownSample;
        int blurH = displayHeight/blurDownSample;
        if (blurDownSample != 1) {
            if (blurW%2!=0)
                blurW++;
            if (blurH%2!=0)
                blurH++;
            if (blurW<1)blurW=1;
            if (blurH<1)blurH=1;
        }
        blur1 = new FrameBuffer(blurW, blurH);
        blur1.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        blur1.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        blur1.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        blur1.setup();
        blur2 = new FrameBuffer(blurW, blurH);
        blur2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        blur2.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        blur2.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        blur2.setup();
        deferred = new FrameBuffer(displayWidth, displayHeight);
        deferred.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        deferred.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        deferred.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        deferred.setup();
    }

    void releaseShaders() {
        if (shaderBlur != null) {
            shaderBlur.release();
            shaderBlur = null;
        }
        if (shaderDeferred != null) {
            shaderDeferred.release();
            shaderDeferred = null;
        }
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
        if (deferred != null) {
            deferred.cleanUp();
            deferred = null;
        }
        if (blur1 != null) {
            blur1.cleanUp();
            blur1 = null;
        }
        if (blur2 != null) {
            blur2.cleanUp();
            blur2 = null;
        }
        Engine.setSceneFB(null);
    }


    public void release() {
        releaseShaders();
        releaseFrameBuffers();
    }


    public void init() {
        initShaders();
    }
}
