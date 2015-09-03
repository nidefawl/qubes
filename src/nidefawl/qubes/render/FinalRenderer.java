package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import nidefawl.game.GL;
import nidefawl.qubes.Client;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.profile.GPUProfiler;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.world.Light;
import nidefawl.qubes.world.World;

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

    public void renderDeferred(World world, float fTime) {

        if (Client.DO_TIMING)
            TimingHelper.startSec("bindFramebuffer");
        deferred.bind();
        if (Client.DO_TIMING)
            TimingHelper.endStart("clearFrameBuffer");
        deferred.clearFrameBuffer();

        if (Client.DO_TIMING)
            TimingHelper.endStart("enableShader");
        shaderDeferred.enable();
        ArrayList<Light> lights = world.lights;
        shaderDeferred.setProgramUniform1i("numLights", Math.min(256, lights.size()));
        for (int a = 0; a < lights.size() && a < 256; a++) {
            Light light = lights.get(a);
            shaderDeferred.setProgramUniform3f("lights["+a+"].Position", light.loc);
            shaderDeferred.setProgramUniform3f("lights["+a+"].Color", light.color);
            float constant = 1.0f;
            float linear = 0.7f;
            float quadratic = 1.8f;
            shaderDeferred.setProgramUniform1f("lights["+a+"].Linear", linear);
            shaderDeferred.setProgramUniform1f("lights["+a+"].Quadratic", quadratic);
            float lightThreshold = 1.0f;
            float maxBrightness = Math.max(Math.max(light.color.x, light.color.y), light.color.z);
            float lightL = (float) (linear * linear - 4 * quadratic * (constant - (256.0 / lightThreshold) * maxBrightness));
            float radius = (-linear + GameMath.sqrtf(lightL)) / (2 * quadratic);
            shaderDeferred.setProgramUniform1f("lights["+a+"].Radius", radius);

            /*
             *             // Update attenuation parameters and calculate radius
            const GLfloat constant = 1.0; // Note that we don't send this to the shader, we assume it is always 1.0 (in our case)
            const GLfloat linear = 0.7;
            const GLfloat quadratic = 1.8;
            glUniform1f(glGetUniformLocation(shaderLightingPass.Program, ("lights[" + std::to_string(i) + "].Linear").c_str()), linear);
            glUniform1f(glGetUniformLocation(shaderLightingPass.Program, ("lights[" + std::to_string(i) + "].Quadratic").c_str()), quadratic);
            // Then calculate radius of light volume/sphere
            const GLfloat lightThreshold = 5.0; // 5 / 256
            const GLfloat maxBrightness = std::fmaxf(std::fmaxf(lightColors[i].r, lightColors[i].g), lightColors[i].b);
            GLfloat radius = (-linear + static_cast<float>(std::sqrt(linear * linear - 4 * quadratic * (constant - (256.0 / lightThreshold) * maxBrightness)))) / (2 * quadratic);
            glUniform1f(glGetUniformLocation(shaderLightingPass.Program, ("lights[" + std::to_string(i) + "].Radius").c_str()), radius);
             */
        }
        if (Client.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderDeferred");

        if (Client.DO_TIMING)
            TimingHelper.endStart("bindTextures");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getNoise());

        if (Client.DO_TIMING)
            TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();

        if (Client.DO_TIMING)
            TimingHelper.endSec();

        if (Client.show) {
            Shader.disable();
            FrameBuffer.unbindFramebuffer();
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Client.DO_TIMING)
                TimingHelper.startSec("DebugOverlayDef");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(0, 0, 0, Engine.getSceneFB().getTexture(0), "texColor");
            dbg.drawDbgTexture(0, 0, 1, Engine.getSceneFB().getTexture(1), "texNormals");
            dbg.drawDbgTexture(0, 0, 2, Engine.getSceneFB().getTexture(2), "texMaterial");
            dbg.drawDbgTexture(0, 0, 3, Engine.getSceneFB().getDepthTex(), "texDepth");
            dbg.drawDbgTexture(0, 0, 4, Engine.shadowRenderer.getDebugTexture(), "texShadow");
            dbg.drawDbgTexture(0, 0, 5, TMgr.getNoise(), "noiseTex");
            dbg.drawDbgTexture(0, 1, 0, this.deferred.getTexture(0), "DeferredOut");
            dbg.postDbgFB();
            if (Client.DO_TIMING)
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
    public void renderBlur(World world, float fTime) {
        int kawaseKernSizeSetting = 2;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        
        int input = this.deferred.getTexture(0);
        FrameBuffer buffer = blur1;

        if (Client.DO_TIMING) TimingHelper.startSec("enableShader");
        shaderBlur.enable();
        if (Client.GL_ERROR_CHECKS) Engine.checkGLError("enable shaderBlur");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        shaderThreshold.enable();
        if (Client.DO_TIMING) TimingHelper.endStart("bindFramebuffer");
        buffer.bind();
        if (Client.DO_TIMING) TimingHelper.endStart("clearFrameBuffer");
        buffer.clearFrameBuffer();
        if (Client.DO_TIMING)  TimingHelper.endStart("bindTexture");
        float w, h;
        w = deferred.getWidth();
        h = deferred.getHeight();
//        if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
            GL11.glViewport(0, 0, buffer.getWidth(), buffer.getHeight());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        if (Client.DO_TIMING) TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();
        input = buffer.getTexture(0);
        buffer = buffer == blur1 ? blur2 : blur1;
        shaderBlur.enable();
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            if (Client.DO_TIMING) TimingHelper.endStart("setuniforms");
            shaderBlur.setProgramUniform3f("blurPassProp", 1.0F/w, 1.0F/h, kawaseKernPasses[p]);
            if (Client.DO_TIMING) TimingHelper.endStart("bindFramebuffer");
            buffer.bind();
            if (Client.DO_TIMING) TimingHelper.endStart("clearFrameBuffer");
            buffer.clearFrameBuffer();
            if (Client.DO_TIMING)  TimingHelper.endStart("bindTexture");
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
            if (Client.DO_TIMING) TimingHelper.endStart("drawFullscreenQuad");
            Engine.drawFullscreenQuad();
            


            if (Client.show) {
//                if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
                    GL11.glViewport(0, 0, deferred.getWidth(), deferred.getHeight());
                FrameBuffer.unbindFramebuffer();
                Shader.disable();
                GuiOverlayDebug dbg = Main.instance.debugOverlay;
                if (Client.DO_TIMING)
                    TimingHelper.startSec("DebugOverlayBlur");
                dbg.preDbgFB(false);
                dbg.drawDbgTexture(1, 0, p+1, input, "pass"+p+" in");
                dbg.drawDbgTexture(1, 1, p+1, buffer.getTexture(0), "pass"+p+" out");
                dbg.postDbgFB();
                if (Client.DO_TIMING)
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
        if (Client.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Client.DO_TIMING)
                TimingHelper.startSec("DebugOverlayBlur");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(1, 0, 0, deferred.getTexture(0), "texColor");
            dbg.drawDbgTexture(1, 1, 0, input, "blured");
            dbg.postDbgFB();
            if (Client.DO_TIMING)
                TimingHelper.endSec();
        }
        this.blurTexture = input;
        if (Client.DO_TIMING) TimingHelper.endSec();
    }


    public void render(World world, float fTime) {

        if (Client.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
        }
        if (Client.DO_TIMING)
            TimingHelper.startSec("Deferred");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Deferred");
        renderDeferred(world, fTime);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (Client.DO_TIMING)
            TimingHelper.endStart("Blur");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Blur");
        renderBlur(world, fTime);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (Client.DO_TIMING)
            TimingHelper.endSec();
        FrameBuffer.unbindFramebuffer();

    }


    public void renderFinal(World world, float fTime) {
        if (Client.show) {
            GuiOverlayDebug dbg = Main.instance.debugOverlay;
            if (Client.DO_TIMING)
                TimingHelper.startSec("DebugOverlayFinal");
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, deferred.getTexture(0), "texColor");
            dbg.drawDbgTexture(2, 0, 1, blurTexture, "texBlur");
            dbg.postDbgFB();
            if (Client.DO_TIMING)
                TimingHelper.endSec();
        }

        if (Client.DO_TIMING)
            TimingHelper.endStart("enableShader");
        shaderFinal.enable();
        if (Client.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        if (Client.DO_TIMING)
            TimingHelper.endStart("bindTexture");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, deferred.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);

        if (Client.DO_TIMING)
            TimingHelper.endStart("drawFullscreenQuad");
        Engine.drawFullscreenQuad();

        if (Client.DO_TIMING)
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
