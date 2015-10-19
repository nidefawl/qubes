package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class FinalRenderer {

    public Shader shaderFinal;
    public Shader shaderBlur;
    public Shader shaderDeferred;
    public Shader shaderThreshold;
    public Shader shaderSSR;

    private FrameBuffer sceneFB;
    public FrameBuffer ssrpass;
    public FrameBuffer deferred;
    private FrameBuffer blur1;
    private FrameBuffer blur2;
    private int         blurTexture;
    private boolean startup = true;
    private int ssr = 0;
    
    /** deferred shader uniform array for lights
     * Layout:
    vec3 Position;
    vec3 Color;
    float Linear;
    float Quadratic;
    float Radius;
     */
    private Uniform3f[] lightColors = new Uniform3f[256];
    private Uniform3f[] lightPos = new Uniform3f[256];
    private Uniform1f[] lightLin = new Uniform1f[256];
    private Uniform1f[] lightExp = new Uniform1f[256];
    private Uniform1f[] lightSize = new Uniform1f[256];

    public void renderDeferred(World world, float fTime, int pass) {

        shaderDeferred.enable();
        ArrayList<DynamicLight> lights = world.lights;
        shaderDeferred.setProgramUniform1i("pass", pass);
        shaderDeferred.setProgramUniform1i("numLights", Math.min(256, lights.size()));
        for (int a = 0; a < lights.size() && a < 256; a++) {
            DynamicLight light = lights.get(a);
            float constant = 1.0f;
            float linear = 0.7f;
            float quadratic = 0.8f;
            float lightThreshold = 0.01f;
            float maxBrightness = Math.max(Math.max(light.color.x, light.color.y), light.color.z);
            float lightL = (float) (linear * linear - 4 * quadratic * (constant - (256.0 / lightThreshold) * maxBrightness));
            float radius = (-linear + GameMath.sqrtf(lightL)) / (2 * quadratic);
            lightPos[a].set(light.loc);
            lightColors[a].set(light.color);
            lightLin[a].set(linear);
            lightExp[a].set(quadratic);
            lightSize[a].set(radius);
        }
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderDeferred");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));

        Engine.drawFullscreenQuad();

        if (Game.show) {
            Shader.disable();
            FrameBuffer.unbindFramebuffer();
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(0, 0, 0, Engine.getSceneFB().getTexture(0), "texColor");
            dbg.drawDbgTexture(0, 0, 1, Engine.getSceneFB().getTexture(1), "texNormals");
            dbg.drawDbgTexture(0, 0, 2, Engine.getSceneFB().getTexture(2), "texMaterial");
            dbg.drawDbgTexture(0, 0, 3, Engine.getSceneFB().getDepthTex(), "texDepth");
            dbg.drawDbgTexture(0, 0, 4, Engine.shadowRenderer.getDebugTexture(), "texShadow");
            dbg.drawDbgTexture(0, 0, 5, TMgr.getNoise(), "noiseTex");
            dbg.drawDbgTexture(0, 0, 6, Engine.getSceneFB().getTexture(3), "light");
            dbg.drawDbgTexture(0, 1, 0, this.deferred.getTexture(0), "DeferredOut");
            dbg.postDbgFB();
        }

    }
    public void bindFB() {
        deferred.bind();
    }
    public void clearFrameBuffer() {
        deferred.clearFrameBuffer();
    }
    final int[][] kawaseKernelSizePasses = new int[] [] {
        {0,0},
        {0, 1, 1},
        {0, 1, 2, 2, 3},
        {0, 1, 2, 3, 4, 4, 5},
        {0, 1, 2, 3, 4, 5, 7, 8, 9, 10},
    };
    private FloatBuffer scaleMatBuf;
    
    public void renderBlur(World world, float fTime) {
        int kawaseKernSizeSetting = 2;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        
        int input = ssr > 0 ? this.ssrpass.getTexture(0) : this.deferred.getTexture(0);
        FrameBuffer buffer = blur1;

        shaderBlur.enable();
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("enable shaderBlur");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
        shaderThreshold.enable();
        buffer.bind();
        buffer.clearFrameBuffer();
        float w, h;
        w = deferred.getWidth();
        h = deferred.getHeight();
//        if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
            GL11.glViewport(0, 0, buffer.getWidth(), buffer.getHeight());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
        Engine.drawFullscreenQuad();
        input = buffer.getTexture(0);
        buffer = buffer == blur1 ? blur2 : blur1;
        shaderBlur.enable();
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            shaderBlur.setProgramUniform3f("blurPassProp", 1.0F/w, 1.0F/h, kawaseKernPasses[p]);
            buffer.bind();
            buffer.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
            Engine.drawFullscreenQuad();
            


            if (Game.show) {
//                if (buffer.getHeight() != deferred.getHeight() || buffer.getWidth() != deferred.getWidth())
                    GL11.glViewport(0, 0, deferred.getWidth(), deferred.getHeight());
                FrameBuffer.unbindFramebuffer();
                Shader.disable();
                GuiOverlayDebug dbg = Game.instance.debugOverlay;
                dbg.preDbgFB(false);
                dbg.drawDbgTexture(1, 0, p+1, input, "pass"+p+" in");
                dbg.drawDbgTexture(1, 1, p+1, buffer.getTexture(0), "pass"+p+" out");
                dbg.postDbgFB();
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
        if (Game.show) {
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(1, 0, 0, deferred.getTexture(0), "texColor");
            dbg.drawDbgTexture(1, 1, 0, input, "blured");
            dbg.postDbgFB();
        }
        this.blurTexture = input;
    }


    public void renderDefa(World world, float fTime) {

        if (Game.show) {
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
        }
        
        if (Game.DO_TIMING)
            TimingHelper.startSec("Deferred");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Deferred");
        renderDeferred(world, fTime, 0);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (ssr > 0) {
            if (Game.DO_TIMING)
                TimingHelper.endStart("SSR");
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SSR");
            renderReflection(world, fTime);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        }
        if (Game.DO_TIMING)
            TimingHelper.endStart("Blur");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Blur");
        renderBlur(world, fTime);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        
        FrameBuffer.unbindFramebuffer();

    }

    public void render(World world, float fTime, int pass) {

        if (Game.DO_TIMING)
            TimingHelper.startSec("Deferred");
        renderDeferred(world, fTime, pass);

    }

    public void renderReflAndBlur(World world, float fTime) {
        if (ssr > 0) {
            if (Game.DO_TIMING)
                TimingHelper.endStart("SSR");
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SSR");
            renderReflection(world, fTime);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        }
        if (Game.DO_TIMING)
            TimingHelper.endStart("Blur");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Blur");
        renderBlur(world, fTime);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (Game.DO_TIMING)
            TimingHelper.endSec();
    }

    /**
     * @param world
     * @param fTime
     */
    private void renderReflection(World world, float fTime) {
        
        ssrpass.bind();
        ssrpass.clearFrameBuffer();
        shaderSSR.enable();
        
        shaderSSR.setProgramUniformMatrix4ARB("inverseProj", false, Engine.getMatSceneP().getInv(), false);
        shaderSSR.setProgramUniformMatrix4ARB("pixelProj", false, scaleMatBuf, false);
        shaderSSR.setProgramUniformMatrix4ARB("worldMVP", false, Engine.getMatSceneMVP().get(), false);
        shaderSSR.setProgramUniform1i("texColor", 0);
        shaderSSR.setProgramUniform1i("texNormals", 1);
        shaderSSR.setProgramUniform1i("texMaterial", 2);
        shaderSSR.setProgramUniform1i("texDepth", 3);
        shaderSSR.setProgramUniform1i("texShadow", 4);
        shaderSSR.setProgramUniform1i("texShadow2", 6);
        shaderSSR.setProgramUniform1i("noisetex", 5);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, deferred.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getNoise());
        Engine.drawFullscreenQuad();
        Shader.disable();
    }


    public void renderFinal(World world, float fTime) {
        if (Game.show) {
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, (ssr > 0 ? ssrpass.getTexture(0) : deferred.getTexture(0)), "texColor");
            dbg.drawDbgTexture(2, 0, 1, blurTexture, "texBlur");
            dbg.postDbgFB();
        }

        shaderFinal.enable();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, (ssr > 0 ? ssrpass.getTexture(0) : deferred.getTexture(0)));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);

        Engine.drawFullscreenQuad();

        Shader.disable();

    }


    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderSSR = assetMgr.loadShader("shaders/basic/ssr", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SSR".equals(define)) {
                        int ssr = Game.instance.settings.ssr;
                        return "#define SSR_"+(ssr<1?1:ssr>3?3:ssr);
                    }
                    return null;
                }
            });
            Shader new_shaderDef = assetMgr.loadShader("shaders/basic/deferred");
            Shader new_shaderBlur = assetMgr.loadShader("shaders/basic/blur_kawase");
            Shader new_shaderFinal = assetMgr.loadShader("shaders/basic/finalstage");
            Shader new_shaderThresh = assetMgr.loadShader("shaders/basic/thresholdfilter");
            releaseShaders();
            shaderFinal = new_shaderFinal;
            shaderBlur = new_shaderBlur;
            shaderDeferred = new_shaderDef;
            shaderThreshold = new_shaderThresh;
            shaderSSR = new_shaderSSR;
            shaderSSR.enable();
            shaderFinal.enable();
            shaderFinal.setProgramUniform1i("texColor", 0);
            shaderFinal.setProgramUniform1i("texBlur", 1);
            shaderBlur.enable();
            shaderBlur.setProgramUniform1i("texColor", 0);
            shaderThreshold.enable();
            shaderThreshold.setProgramUniform1i("texColor", 0);
            shaderThreshold.setProgramUniform1i("texLight", 6);
            shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texShadow2", 4);
            shaderDeferred.setProgramUniform1i("noisetex", 5);
            shaderDeferred.setProgramUniform1i("texLight", 6);
            for (int i = 0; i < lightColors.length; i++) {
                this.lightPos[i] = shaderDeferred.getUniform("lights["+i+"].Position", Uniform3f.class);
                this.lightColors[i] = shaderDeferred.getUniform("lights["+i+"].Color", Uniform3f.class);
                this.lightLin[i] = shaderDeferred.getUniform("lights["+i+"].Linear", Uniform1f.class);
                this.lightExp[i] = shaderDeferred.getUniform("lights["+i+"].Quadratic", Uniform1f.class);
                this.lightSize[i] = shaderDeferred.getUniform("lights["+i+"].Radius", Uniform1f.class);
            }
            Shader.disable();
        } catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }


    public void resize(int displayWidth, int displayHeight) {
        releaseFrameBuffers();
        sceneFB = new FrameBuffer(displayWidth, displayHeight);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
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
        
        
        

        ssrpass = new FrameBuffer(displayWidth, displayHeight);
        ssrpass.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        ssrpass.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        ssrpass.setup();
        
        Matrix4f trs = new Matrix4f();
        trs.translate(0.5f, 0.5f, 0);
        trs.scale(new Vector3f(0.5f, 0.5f, 1.0F));
        Matrix4f scale = new Matrix4f();
        scale.scale(new Vector3f(Game.displayWidth, Game.displayHeight, 1));
        Matrix4f.mul(scale, trs, scale);
        Matrix4f.mul(scale, Engine.getMatSceneP(), scale);
        if (this.scaleMatBuf == null)
            this.scaleMatBuf=Memory.createFloatBuffer(16);
        this.scaleMatBuf.position(0);
        scale.store(scaleMatBuf);
        scaleMatBuf.flip();
        
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
        if (shaderSSR != null) {
            shaderSSR.release();
            shaderSSR = null;
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
        if (ssrpass != null) {
            ssrpass.cleanUp();
            ssrpass = null;
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
        setSSR(Game.instance.settings.ssr);
    }
    /**
     * @param id
     */
    public void setSSR(int ssr) {
        this.ssr = ssr;
        initShaders();
    }
}
