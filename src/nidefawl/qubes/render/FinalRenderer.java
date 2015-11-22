package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class FinalRenderer extends AbstractRenderer {

    public Shader       shaderFinal;
    public Shader       shaderFinalNoBloom;
    public Shader       shaderBlur;
    public Shader       shaderDeferred;
    public Shader       shaderInterpLum;
    public Shader       shaderThreshold;
    public Shader       shaderSSR;
    public Shader       shaderSSRBlur;
    public Shader       shaderSSRCombine;
    public Shader       shaderDownsample4x;
    public Shader       shaderDownsample4xLum;

    private FrameBuffer fbScene;
    public FrameBuffer  fbSSR;
    public FrameBuffer  fbSSRBlurredX;
    public FrameBuffer  fbSSRBlurredY;
    public FrameBuffer  fbSSRCombined;
    public FrameBuffer  fbDeferred;
    public FrameBuffer  fbSSAO;
    public FrameBuffer  fbBloom;
    private FrameBuffer fbBlur1;
    private FrameBuffer fbBlur2;
    private FrameBuffer fbLuminanceDownsample[];
    private FrameBuffer fbLuminanceInterp[];

    private int         blurTexture;
    private boolean     startup     = true;
    private int         ssr         = 0;
    private FloatBuffer floatBuf;
    float               curBrightness;
    float               brightness;
    private int         frame;
    SMAA smaa;
    
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
            lightPos[a].set(light.loc.x-Engine.GLOBAL_OFFSET.x, light.loc.y-Engine.GLOBAL_OFFSET.y, light.loc.z-Engine.GLOBAL_OFFSET.z);
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
        GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, this.fbSSAO.getTexture(0));

        Engine.drawFullscreenQuad();

        if (Game.show && pass == 0) {
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
            dbg.drawDbgTexture(0, 1, 0, this.fbDeferred.getTexture(0), "DeferredOut");
            dbg.drawDbgTexture(0, 1, 1, this.fbSSAO.getTexture(0), "AOOut");
            dbg.postDbgFB();
        }

    }
    public void bindFB() {
        fbDeferred.bind();
    }
    public void clearFrameBuffer() {
        fbDeferred.clearFrameBuffer();
    }
    final int[][] kawaseKernelSizePasses = new int[] [] {
        {0,0},
        {0, 1, 1},
        {0, 1, 2, 2, 3},
        {0, 1, 2, 3, 4, 4, 5},
        {0, 1, 2, 3, 4, 5, 7, 8, 9, 10},
    };
    private FloatBuffer scaleMatBuf;
    private boolean hadContext;
    
    public void calcLum(World world, float fTime) {
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance");
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance 4x + convert");
        glDisable(GL_BLEND);
        FrameBuffer inputBuffer = ssr > 0 ? this.fbSSRCombined : this.fbDeferred;
        
        
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, inputBuffer.getTexture(0)); // Albedo
        GL11.glViewport(0, 0, fbBlur1.getWidth(), fbBlur1.getHeight()); // 4x downsampled render resolutino
        
        float twoPixelX = 2.0f/(float)inputBuffer.getWidth();
        float twoPixelY = 2.0f/(float)inputBuffer.getHeight();
        shaderDownsample4xLum.enable();
        shaderDownsample4xLum.setProgramUniform2f("twoTexelSize", twoPixelX, twoPixelY);
        this.fbLuminanceDownsample[0].bind();
        this.fbLuminanceDownsample[0].clearFrameBuffer();
        Engine.drawFullscreenQuad();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        shaderDownsample4x.enable();
        FrameBuffer lastBound = null;
        for (int i = 0; i < this.fbLuminanceDownsample.length-1; i++) {
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance step "+(i));
//            System.out.println("input w/h "+this.fbLuminance[i].getWidth()+", "+this.fbLuminance[i].getHeight());
            twoPixelX = 2.0f / (float) this.fbLuminanceDownsample[i].getWidth();
            twoPixelY = 2.0f / (float) this.fbLuminanceDownsample[i].getHeight();
            lastBound = this.fbLuminanceDownsample[i+1];
            GL11.glViewport(0, 0, lastBound.getWidth(), lastBound.getHeight()); // 16x downsampled render resolutino
            shaderDownsample4x.setProgramUniform2f("twoTexelSize", twoPixelX, twoPixelY);
            lastBound.bind();
            lastBound.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceDownsample[i].getTexture(0));
            Engine.drawFullscreenQuad();
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        }
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance Interp");
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        this.fbLuminanceInterp[indexOut].bind();
        this.fbLuminanceInterp[indexOut].clearFrameBuffer();
        this.shaderInterpLum.enable();
        this.shaderInterpLum.setProgramUniform1f("elapsedTime", (Stats.avgFrameTime)/100f);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceInterp[indexIn].getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, lastBound.getTexture(0));
        Engine.drawFullscreenQuad();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
    }
    public void renderBlur(World world, float fTime) {
        FrameBuffer inputBuffer = ssr > 0 ? this.fbSSRCombined : this.fbDeferred;

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Bloom Blur");
        GL11.glViewport(0, 0, fbBlur1.getWidth(), fbBlur1.getHeight()); // 4x downsampled render resolutino
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, inputBuffer.getTexture(0)); // Albedo
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3)); // Material/Blockinfo
        
        shaderThreshold.enable();
        fbBlur1.bind();
        fbBlur1.clearFrameBuffer();
        Engine.drawFullscreenQuad();
        this.blurTexture = blur(fbBlur1.getTexture(0), fbBlur1.getWidth(), fbBlur1.getHeight(), null);
        
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        GL11.glViewport(0, 0, fbDeferred.getWidth(), fbDeferred.getHeight());
        glEnable(GL_BLEND);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
    }
    
    int blur(int input, int w, int h, FrameBuffer target) {
        FrameBuffer buffer = input == fbBlur1.getTexture(0) ? fbBlur2 : fbBlur1; // 4x downsampled
        shaderBlur.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input); // Albedo
        int kawaseKernSizeSetting = 2;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        float w1 = 1.0f/w;
        float h1 = 1.0f/h;
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            shaderBlur.setProgramUniform3f("blurPassProp", w1, h1, kawaseKernPasses[p]);
            buffer.bind();
            buffer.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
            Engine.drawFullscreenQuad();
            input = buffer.getTexture(0);
            buffer = buffer == fbBlur1 ? fbBlur2 : fbBlur1;
            if (target != null && p == kawaseKernPasses.length-2) {
                if (buffer == target) {
                    throw new GameError("INPUT == OUTPUT FB!");
                }
                buffer = target;
            }
            int nextw = buffer.getWidth();
            int nexth = buffer.getHeight();
            if (w != nextw || h != nexth) {
                GL11.glViewport(0, 0, nextw, nexth);
            }
            w = nextw;
            h = nexth;
        }
        return input;
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
            renderReflection(world, fTime);
        }
        if (Game.DO_TIMING)
            TimingHelper.endStart("calcLum");
        calcLum(world, fTime);
        if (Game.DO_TIMING)
            TimingHelper.endStart("Blur");
        renderBlur(world, fTime);
        if (Game.DO_TIMING)
            TimingHelper.endSec();
    }

    /**
     * @param world
     * @param fTime
     */
    private void renderReflection(World world, float fTime) {

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SSR");
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("raytrace");
        fbSSR.bind();
        fbSSR.clearFrameBuffer();
        shaderSSR.enable();
        shaderSSR.setProgramUniformMatrix4("inverseProj", false, Engine.getMatSceneP().getInv(), false);
        shaderSSR.setProgramUniformMatrix4("pixelProj", false, scaleMatBuf, false);
        shaderSSR.setProgramUniformMatrix4("worldMVP", false, Engine.getMatSceneMVP().get(), false);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0)); //COLOR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1)); //NORMAL
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2)); //MATERIAL
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex()); //DEPTH
        Engine.drawFullscreenQuad();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
        
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("blur");
        
        float maxBlurRadius = 8;
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbSSR.getTexture(0)); //SSR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1)); //NORMAL
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex()); //DEPTH
        fbSSRBlurredX.bind();
        fbSSRBlurredX.clearFrameBuffer();
        shaderSSRBlur.enable();
        shaderSSRBlur.setProgramUniform2f("_TexelOffsetScale", maxBlurRadius / (float)fbSSR.getWidth(), 0f);
        Engine.drawFullscreenQuad();
        fbSSRBlurredY.bind();
        fbSSRBlurredY.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbSSRBlurredX.getTexture(0)); //COLOR
        shaderSSRBlur.setProgramUniform2f("_TexelOffsetScale", 0f, maxBlurRadius / (float)fbSSR.getHeight());
        Engine.drawFullscreenQuad();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
        
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("combine");
        shaderSSRCombine.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0)); //COLOR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, fbSSRBlurredY.getTexture(0)); //Blurred
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2)); //MATERIAL
        fbSSRCombined.bind();
        fbSSRCombined.clearFrameBuffer();
        Engine.drawFullscreenQuad();
        Shader.disable();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
    }


    public void renderFinal(World world, float fTime) {
        FrameBuffer input = ssr > 0 ? fbSSRCombined : fbDeferred;
        if (Game.show) {
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, input.getTexture(0), "texColor");
            dbg.drawDbgTexture(2, 0, 1, blurTexture, "texBlur");
            dbg.postDbgFB();
        }
        
        
        shaderFinal.enable();
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, this.fbLuminanceInterp[indexOut].getTexture(0));
        boolean aa = Game.instance.settings.aa>0;
        if (!aa) {
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("combine bloom");
            FrameBuffer.unbindFramebuffer();
            Engine.drawFullscreenQuad();
            Shader.disable();
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        } else {
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("combine bloom");
            glDisable(GL_BLEND);
            fbBloom.bind();
            Engine.drawFullscreenQuad();
            if (Game.show) {
                GuiOverlayDebug dbg = Game.instance.debugOverlay;
                dbg.preDbgFB(false);
                dbg.drawDbgTexture(2, 1, 0, fbBloom.getTexture(0), "bloom+albedo");
                dbg.drawDbgTexture(3, 0, 0, fbBloom.getTexture(0), "bloom+albedo");
                dbg.postDbgFB();
            }
            FrameBuffer.unbindFramebuffer();
            int outputColor = fbBloom.getTexture(0);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SMAA");
            this.smaa.render(outputColor, 0);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        }
//        Shaders.textured.enable();
//        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbScene.getTexture(1));
//        Engine.drawFullscreenQuad();

        this.frame++;
    }


    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderSSR = assetMgr.loadShader(this, "post/SSR/ssr", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SSR".equals(define)) {
                        int ssr = Game.instance.settings.ssr;
                        return "#define SSR_"+(ssr<1?1:ssr>3?3:ssr);
                    }
                    return null;
                }
            });
            Shader new_shaderSSRBlur = assetMgr.loadShader(this, "post/SSR/ssr_blur");
            Shader new_shaderSSRCombine = assetMgr.loadShader(this, "post/SSR/ssr_combine");
            Shader new_shaderDef = assetMgr.loadShader(this, "post/deferred");
            Shader new_shaderBlur = assetMgr.loadShader(this, "filter/blur_kawase");
            Shader new_shaderFinal = assetMgr.loadShader(this, "post/finalstage", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("DO_BLOOM".equals(define)) {
                        return "#define DO_BLOOM";
                    }
                    return null;
                }
            });
            Shader new_shaderFinalNoBloom = assetMgr.loadShader(this, "post/finalstage");
            
//            Shader new_calcLuminance = assetMgr.loadShader(this, "shaders/basic/calcLuminance");
//            Shader new_calcAdaptedLuminance = assetMgr.loadShader(this, "shaders/basic/calcAdaptedLuminance");
            Shader new_shaderDownsample4x = assetMgr.loadShader(this, "filter/downsample4x");
            Shader new_shaderDownsample4xLum = assetMgr.loadShader(this, "filter/downsample4x", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("LUMINANCE".equals(define))
                        return "#define LUMINANCE";
                    return null;
                }
            });
            
            Shader new_shaderThresh = assetMgr.loadShader(this, "filter/thresholdfilter");
            
            Shader new_interpLum = assetMgr.loadShader(this, "filter/luminanceInterp");
            popNewShaders();
            shaderSSRCombine = new_shaderSSRCombine;
            shaderSSRBlur = new_shaderSSRBlur;
            shaderFinal = new_shaderFinal;
            shaderFinalNoBloom = new_shaderFinalNoBloom;
            shaderBlur = new_shaderBlur;
//            shaderCalcLuminance = new_calcLuminance;
//            shaderCalcAdaptedLuminance = new_calcAdaptedLuminance;
            shaderDownsample4x = new_shaderDownsample4x;
            shaderDownsample4xLum = new_shaderDownsample4xLum;
            shaderDeferred = new_shaderDef;
            shaderInterpLum = new_interpLum;
            shaderThreshold = new_shaderThresh;
            shaderSSR = new_shaderSSR;
            shaderSSR.enable();
            shaderSSR.setProgramUniform1i("texColor", 0);
            shaderSSR.setProgramUniform1i("texNormals", 1);
            shaderSSR.setProgramUniform1i("texMaterial", 2);
            shaderSSR.setProgramUniform1i("texDepth", 3);
            shaderSSRBlur.enable();
            shaderSSRBlur.setProgramUniform1i("texSSR", 0);
            shaderSSRBlur.setProgramUniform1i("texNormals", 1);
            shaderSSRBlur.setProgramUniform1i("texDepth", 3);
            shaderSSRCombine.enable();
            shaderSSRCombine.setProgramUniform1i("texColor", 0);
            shaderSSRCombine.setProgramUniform1i("texSSRBlurred", 1);
            shaderSSRCombine.setProgramUniform1i("texMaterial", 2);
            shaderInterpLum.enable();
            shaderInterpLum.setProgramUniform1i("texPrev", 0);
            shaderInterpLum.setProgramUniform1i("texNew", 1);
            shaderDownsample4x.enable();
            shaderDownsample4x.setProgramUniform1i("texColor", 0);
            shaderDownsample4xLum.enable();
            shaderDownsample4xLum.setProgramUniform1i("texColor", 0);
            
            shaderFinal.enable();
            shaderFinal.setProgramUniform1i("texColor", 0);
            shaderFinal.setProgramUniform1i("texBlur", 1);
            shaderFinal.setProgramUniform1i("texLum", 2);
            shaderFinalNoBloom.enable();
            shaderFinalNoBloom.setProgramUniform1i("texColor", 0);
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
            shaderDeferred.setProgramUniform1i("texAO", 7);
            for (int i = 0; i < lightColors.length; i++) {
                this.lightPos[i] = shaderDeferred.getUniform("lights["+i+"].Position", Uniform3f.class);
                this.lightColors[i] = shaderDeferred.getUniform("lights["+i+"].Color", Uniform3f.class);
                this.lightLin[i] = shaderDeferred.getUniform("lights["+i+"].Linear", Uniform1f.class);
                this.lightExp[i] = shaderDeferred.getUniform("lights["+i+"].Quadratic", Uniform1f.class);
                this.lightSize[i] = shaderDeferred.getUniform("lights["+i+"].Radius", Uniform1f.class);
            }
            Shader.disable();
        } catch (ShaderCompileError e) {
            releaseNewShaders();
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

    /**
     * 
     */
    public void initAA() {
        if (smaa != null) {
            this.smaa.releaseAll(null);
        }
        smaa = new SMAA(Game.instance.settings.smaaQuality);
        this.smaa.init(Game.displayWidth, Game.displayHeight);
    }

    public void resize(int displayWidth, int displayHeight) {
        if (hadContext) {
            Engine.checkGLError("pre GLNativeLib.deleteContext");
            HBAOPlus.deleteContext();
            Engine.checkGLError("post GLNativeLib.deleteContext");
        }
        if (smaa != null) {
            smaa.releaseAll(EResourceType.FRAMEBUFFER);
        }
        releaseAll(EResourceType.FRAMEBUFFER);
        Engine.checkGLError("pre GLNativeLib.createContext");
        HBAOPlus.createContext(displayWidth, displayHeight);
        Engine.checkGLError("post GLNativeLib.createContext");
        hadContext = true;
        if (smaa == null) {
            smaa = new SMAA(Game.instance.settings.smaaQuality);
        }
        this.smaa.init(displayWidth, displayHeight);
        fbScene = new FrameBuffer(displayWidth, displayHeight);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
        fbScene.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
        fbScene.setHasDepthAttachment();
        fbScene.setup(this);
        Engine.setSceneFB(fbScene);
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
        fbBlur1 = FrameBuffer.make(this, blurW, blurH, GL_RGB16F);
        fbBlur2 = FrameBuffer.make(this, blurW, blurH, GL_RGB16F);
        int lumW = blurW;
        int lumH = blurH;
        List<FrameBuffer> list = Lists.newArrayList();
        while (true) {
            FrameBuffer fbLuminance2 = FrameBuffer.make(this, lumW, lumH, GL_RGB16F);
            list.add(fbLuminance2);
            if (lumW == 1 && lumH == 1) break;
            lumW = lumW / 4;
            lumH = lumH / 4;
            if (lumW < 1)
                lumW = 1;
            if (lumH < 1)
                lumH = 1;
        }
        this.fbLuminanceDownsample = list.toArray(new FrameBuffer[list.size()]);
        this.fbLuminanceInterp = new FrameBuffer[2];
        for (int i = 0; i < 2; i++) {
            this.fbLuminanceInterp[i] = FrameBuffer.make(this, 1, 1, GL_RGB16F);
        }
        if (this.floatBuf == null)
        this.floatBuf = Memory.createFloatBufferAligned(4, 4);
//        FrameBuffer.unbindFramebuffer();
//        list.get(list.size()-1).bind();
//        floatBuf.position(0);
//        glReadBuffer(GL_COLOR_ATTACHMENT0);
//        glReadPixels(0, 0, 1, 1, GL_RED, GL_FLOAT, floatBuf);
//        FrameBuffer.unbindFramebuffer();
//        System.out.println(floatBuf.get(0));

        fbDeferred = FrameBuffer.make(this, displayWidth, displayHeight, GL_RGB16F);
        fbBloom = new FrameBuffer(displayWidth, displayHeight);
        fbBloom.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbBloom.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        fbBloom.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbBloom.setup(this);
        fbBloom.bind();
        fbBloom.clearColor();
        


        fbSSAO = new FrameBuffer(displayWidth, displayHeight);
        fbSSAO.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbSSAO.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
        fbSSAO.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 1F, 1F, 1F);
        fbSSAO.setup(this);
        fbSSAO.bind();
        fbSSAO.clearColor();
        

        fbSSR = new FrameBuffer(displayWidth, displayHeight);
        fbSSR.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSR.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSR.setup(this);
        
        //TODO: test downsampling ssr blur
        fbSSRBlurredX = new FrameBuffer(displayWidth, displayHeight);
        fbSSRBlurredX.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSRBlurredX.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRBlurredX.setup(this);
        fbSSRBlurredY = new FrameBuffer(displayWidth, displayHeight);
        fbSSRBlurredY.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSRBlurredY.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRBlurredY.setup(this);
        fbSSRCombined = new FrameBuffer(displayWidth, displayHeight);
        fbSSRCombined.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbSSRCombined.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRCombined.setup(this);
        
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
        HBAOPlus.setDepthTex(Engine.getSceneFB().getDepthTex());
        HBAOPlus.setNormalTex(Engine.getSceneFB().getTexture(1));
        long ptr = MemoryUtil.memAddress(Engine.getMatSceneP().get());
        HBAOPlus.setProjMatrix(ptr);
        HBAOPlus.setOutputFBO(fbSSAO.getFB());
        HBAOPlus.setRadius(1);
        HBAOPlus.setBias(0.2f);
        HBAOPlus.setCoarseAO(1.2f);
        HBAOPlus.setBlur(true, 8, 16.0f);
        HBAOPlus.setBlurSharpen(false, 16, 0, 0);
        HBAOPlus.setDetailAO(1f);
        HBAOPlus.setPowerExponent(1);
        HBAOPlus.setDepthThreshold(false, 220, 0.5f);
    
    }


    public void release() {
        super.release();
        Engine.setSceneFB(null);
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
