package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class FinalRenderer extends AbstractRenderer {

    public Shader       shaderBloomCombine;
    public Shader       shaderFinal;
    public Shader       shaderBlur;
    public Shader       shaderDeferred;
    public Shader       shaderDeferredWater;
    public Shader       shaderDeferredFirstPerson;
    public Shader       shaderInterpLum;
    public Shader       shaderThreshold;
    public Shader       shaderSSR;
    public Shader       shaderSSRBlur;
    public Shader       shaderSSRCombine;
    public Shader       shaderDownsample4x;
    public Shader       shaderDownsample4xLum;
    private Shader shaderNormals;

    private FrameBuffer fbScene;
    public FrameBuffer  fbSSR;
    public FrameBuffer  fbSSRBlurredX;
    public FrameBuffer  fbSSRBlurredY;
    public FrameBuffer  fbSSRCombined;
    public FrameBuffer  fbDeferred;
    public FrameBuffer  fbSSAO;
    public FrameBuffer  fbFinal;
    private FrameBuffer fbBlur1;
    private FrameBuffer fbBlur2;
    private FrameBuffer fbLuminanceDownsample[];
    private FrameBuffer fbLuminanceInterp[];

    private int         blurTexture;
    private int preWaterDepthTex;
    private boolean     startup     = true;
    private int         ssr         = 0;
    float               curBrightness;
    float               brightness;
    private int         frame;
    SMAA smaa;

    public void renderDeferred(World world, float fTime, int pass) {
        Shader shaderDeferred = this.shaderDeferred;
        if (pass == 1) {
            shaderDeferred = this.shaderDeferredWater;
        }
        if (pass == 2) {
            shaderDeferred = this.shaderDeferredFirstPerson;
        }
        shaderDeferred.enable();
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderDeferred");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.lightCompute.getTexture());
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
        GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, this.fbSSAO.getTexture(0));
        if (pass == 1) {
            GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, this.preWaterDepthTex);
        }

        Engine.drawFullscreenQuad();

        if (GLDebugTextures.isShow()) {
            Shader.disable();
            FrameBuffer.unbindFramebuffer();
            String name;
            switch (pass) {
                case 0:
                    name = "Main";
                    break;
                case 1:
                    name = "Transparent";
                    break;
                case 2:
                    name = "FirstPerson";
                    break;
                default:
                    name = "Pass_"+pass;
                    break;
            }
            GLDebugTextures.readTexture(name, "texColor", Engine.getSceneFB().getTexture(0));
            GLDebugTextures.readTexture(name, "texNormals", Engine.getSceneFB().getTexture(1));
            GLDebugTextures.readTexture(name, "texMaterial", Engine.getSceneFB().getTexture(2));
            GLDebugTextures.readTexture(name, "blocklight", Engine.getSceneFB().getTexture(3));
            GLDebugTextures.readTexture(name, "texDepth", Engine.getSceneFB().getDepthTex(), 2);
            GLDebugTextures.readTexture(name, "DeferredOut", this.fbDeferred.getTexture(0), 1);
            GLDebugTextures.readTexture(name, "light", Engine.lightCompute.getTexture());
            if (pass == 0) {
                GLDebugTextures.readTexture(name, "texShadow", Engine.shadowRenderer.getDebugTexture());
                GLDebugTextures.readTexture(name, "AOOut", this.fbSSAO.getTexture(0));
            }
            if (pass == 1) {
                GLDebugTextures.readTexture(name, "preWaterDepth", this.preWaterDepthTex, 2);
            }
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
    
    public void calcLum(World world, float fTime) {
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance");
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
        shaderDownsample4x.enable();
        FrameBuffer lastBound = this.fbLuminanceDownsample[0];
        for (int i = 0; i < this.fbLuminanceDownsample.length-1; i++) {
//            System.out.println("input w/h "+this.fbLuminanceDownsample[i].getWidth()+", "+this.fbLuminanceDownsample[i].getHeight());
            twoPixelX = 2.0f / (float) this.fbLuminanceDownsample[i].getWidth();
            twoPixelY = 2.0f / (float) this.fbLuminanceDownsample[i].getHeight();
            lastBound = this.fbLuminanceDownsample[i+1];
            GL11.glViewport(0, 0, lastBound.getWidth(), lastBound.getHeight()); // 16x downsampled render resolutino
            shaderDownsample4x.setProgramUniform2f("twoTexelSize", twoPixelX, twoPixelY);
            lastBound.bind();
            lastBound.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceDownsample[i].getTexture(0));
            Engine.drawFullscreenQuad();
        }
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        this.fbLuminanceInterp[indexOut].bind();
        this.fbLuminanceInterp[indexOut].clearFrameBuffer();
        this.shaderInterpLum.enable();
        this.shaderInterpLum.setProgramUniform1f("elapsedTime", (Stats.avgFrameTime)/100f);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceInterp[indexIn].getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, lastBound.getTexture(0));
        Engine.drawFullscreenQuad();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture("Luminance", "Output", this.fbLuminanceInterp[indexOut].getTexture(0));
        }

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
    }
    public void renderBlur(World world, float fTime) {
        FrameBuffer inputBuffer = ssr > 0 ? this.fbSSRCombined : this.fbDeferred;

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Blur");
        GL11.glViewport(0, 0, fbBlur1.getWidth(), fbBlur1.getHeight()); // 4x downsampled render resolutino
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, inputBuffer.getTexture(0)); // Albedo
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3)); // Material/Blockinfo
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.lightCompute.getTexture());
        
        shaderThreshold.enable();
        fbBlur1.bind();
        fbBlur1.clearFrameBuffer();
        Engine.drawFullscreenQuad();
        this.blurTexture = blur(fbBlur1.getTexture(0), fbBlur1.getWidth(), fbBlur1.getHeight(), null);
        
        FrameBuffer.unbindFramebuffer();
        Shader.disable();

        GL11.glViewport(0, 0, fbDeferred.getWidth(), fbDeferred.getHeight());
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
        renderDeferred(world, fTime, pass);
        
    }

    public void renderReflAndBlur(World world, float fTime) {
        if (ssr > 0) {
            renderReflection(world, fTime);
        }
        calcLum(world, fTime);
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
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0)); //COLOR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1)); //NORMAL
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2)); //MATERIAL
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, this.fbFinal.getDepthTex()); //DEPTH
        Engine.drawFullscreenQuad();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
        
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("blur");
        
        float maxBlurRadius = 8;
        fbSSRBlurredX.bind();
        fbSSRBlurredX.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbSSR.getTexture(0)); //SSR
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
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture("SSR", "DeferredInput", fbDeferred.getTexture(0), 1);
            GLDebugTextures.readTexture("SSR", "SSROutput", fbSSR.getTexture(0), 1);
            GLDebugTextures.readTexture("SSR", "SSRBlurred", fbSSRBlurredY.getTexture(0), 1);
            GLDebugTextures.readTexture("SSR", "SSRBlurCombined", fbSSRCombined.getTexture(0), 1);
            GLDebugTextures.readTexture("SSR", "texDepth", this.fbFinal.getDepthTex(), 2);
            GLDebugTextures.readTexture("SSR", "texColor", fbDeferred.getTexture(0));
            GLDebugTextures.readTexture("SSR", "texNormals", Engine.getSceneFB().getTexture(1));
            GLDebugTextures.readTexture("SSR", "texMaterial", Engine.getSceneFB().getTexture(2));
        }
    }


    public void renderFinal(World world, float fTime) {
        int outputColor = fbFinal.getTexture(0);
        if (smaa != null) {
            fbDeferred.bind();
            fbDeferred.clearColorBlack();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, outputColor);
            outputColor = fbDeferred.getTexture(0);
        } else {
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, outputColor);
            FrameBuffer.unbindFramebuffer();
        }
        shaderFinal.enable();
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.fbLuminanceInterp[indexOut].getTexture(0));
        Engine.drawFullscreenQuad();
        if (smaa != null) {
            FrameBuffer.unbindFramebuffer();
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SMAA");
            this.smaa.render(outputColor, 0);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        }

        this.frame++;
        
        
//
//        if(Stats.fpsCounter==0) {
//            Engine.debugOutput.bind();
//            ByteBuffer buf = Engine.debugOutput.map(false);
//            
//            float[] debugVals = new float[16];
//            FloatBuffer fbuf = buf.asFloatBuffer();
//            fbuf.get(debugVals);
//            System.out.println("a "+String.format("%10f", debugVals[0]));
//            System.out.println("b "+String.format("%10f", debugVals[1]));
//            Engine.debugOutput.unmap();
//            Engine.debugOutput.unbind();
//        }
        
    }
    
    public int renderNormals() {
        fbFinal.bind();
        fbFinal.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbScene.getTexture(1));
        this.shaderNormals.enable();
        Engine.drawFullscreenQuad();
        GLDebugTextures.readTexture("fixNormals", "texNormals", Engine.getSceneFB().getTexture(1), 8);
        return GLDebugTextures.readTexture("fixNormals", "output", fbFinal.getTexture(0), 8);
    }
    public void copyPreWaterDepth() {
        FrameBuffer.unbindReadFramebuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.preWaterDepthTex);
        ARBCopyImage.glCopyImageSubData(fbScene.getDepthTex(), GL_TEXTURE_2D, 0, 0, 0, 0, this.preWaterDepthTex, GL_TEXTURE_2D, 0, 0, 0, 0, Game.displayWidth, Game.displayHeight, 1);
        
    }
    public void copySceneDepthBuffer() {
        fbFinal.bind();

        fbScene.bindRead();
        //             
        GL30.glBlitFramebuffer(0, 0, fbScene.getWidth(), fbScene.getHeight(), 0, 0, fbScene.getWidth(), fbScene.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
    }
    public void renderBloom(World world, float fTime) {
        renderBlur(world, fTime);
        FrameBuffer input = ssr > 0 ? fbSSRCombined : fbDeferred;
        if (GLDebugTextures.isShow()) {
            GuiOverlayDebug dbg = Game.instance.debugOverlay;
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, input.getTexture(0), "texColor");
            dbg.drawDbgTexture(2, 0, 1, blurTexture, "texBlur");
            dbg.postDbgFB();
        }
        fbFinal.bind();
//        fbFinal.clearFrameBuffer();
        shaderBloomCombine.enable();
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Combine");

        Engine.drawFullscreenQuad();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture("Bloom", "blurInput", input.getTexture(0));
            GLDebugTextures.readTexture("Bloom", "blurTexture", blurTexture);
            GLDebugTextures.readTexture("Bloom", "fbFinal", fbFinal.getTexture(0));
        }
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
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
            Shader new_shaderDef = assetMgr.loadShader(this, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            Shader new_shaderDef2 = assetMgr.loadShader(this, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 1";
                    }
                    return null;
                }
            });
            Shader new_shaderDef3 = assetMgr.loadShader(this, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 2";
                    }
                    return null;
                }
            });
            Shader new_shaderBlur = assetMgr.loadShader(this, "filter/blur_kawase");
            Shader new_shaderbloom_combine = assetMgr.loadShader(this, "post/bloom_combine", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("DO_BLOOM".equals(define)) {
                        return "#define DO_BLOOM";
                    }
                    return null;
                }
            });
            Shader new_shaderFinal = assetMgr.loadShader(this, "post/finalstage", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("DO_AUTOEXPOSURE".equals(define))
                        return "#define DO_AUTOEXPOSURE";
                    return null;
                }
            });
            
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
            Shader newshaderNormals = assetMgr.loadShader(this, "filter/normals_rh_lh");
            popNewShaders();
            shaderSSRCombine = new_shaderSSRCombine;
            shaderSSRBlur = new_shaderSSRBlur;
            shaderBloomCombine = new_shaderbloom_combine;
            shaderFinal = new_shaderFinal;
            shaderBlur = new_shaderBlur;
            shaderDownsample4x = new_shaderDownsample4x;
            shaderDownsample4xLum = new_shaderDownsample4xLum;
            shaderDeferred = new_shaderDef;
            shaderDeferredWater = new_shaderDef2;
            shaderDeferredFirstPerson = new_shaderDef3;
            shaderInterpLum = new_interpLum;
            shaderThreshold = new_shaderThresh;
            shaderSSR = new_shaderSSR;
            shaderNormals = newshaderNormals;
            shaderNormals.enable();
            shaderNormals.setProgramUniform1i("texNormals", 0);
            shaderSSR.enable();
            shaderSSR.setProgramUniform1i("texColor", 0);
            shaderSSR.setProgramUniform1i("texNormals", 1);
            shaderSSR.setProgramUniform1i("texMaterial", 2);
            shaderSSR.setProgramUniform1i("texDepth", 3);
            if (this.scaleMatBuf != null) {
                this.shaderSSR.setProgramUniformMatrix4("pixelProj", false, scaleMatBuf, false);
            }
            shaderSSRBlur.enable();
            shaderSSRBlur.setProgramUniform1i("texSSR", 0);
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
            
            shaderBloomCombine.enable();
            shaderBloomCombine.setProgramUniform1i("texColor", 0);
            shaderBloomCombine.setProgramUniform1i("texBlur", 1);
            shaderFinal.enable();
            shaderFinal.setProgramUniform1i("texColor", 0);
            shaderFinal.setProgramUniform1i("texLum", 1);
            shaderBlur.enable();
            shaderBlur.setProgramUniform1i("texColor", 0);
            shaderThreshold.enable();
            shaderThreshold.setProgramUniform1i("texColor", 0);
            shaderThreshold.setProgramUniform1i("texLight", 5);
            shaderThreshold.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);
            shaderDeferredWater.enable();
            shaderDeferredWater.setProgramUniform1i("texColor", 0);
            shaderDeferredWater.setProgramUniform1i("texNormals", 1);
            shaderDeferredWater.setProgramUniform1i("texMaterial", 2);
            shaderDeferredWater.setProgramUniform1i("texDepth", 3);
            shaderDeferredWater.setProgramUniform1i("texShadow", 4);
            shaderDeferredWater.setProgramUniform1i("texLight", 5);
            shaderDeferredWater.setProgramUniform1i("texBlockLight", 6);
            shaderDeferredWater.setProgramUniform1i("texAO", 7);
            shaderDeferredFirstPerson.enable();
            shaderDeferredFirstPerson.setProgramUniform1i("texColor", 0);
            shaderDeferredFirstPerson.setProgramUniform1i("texNormals", 1);
            shaderDeferredFirstPerson.setProgramUniform1i("texMaterial", 2);
            shaderDeferredFirstPerson.setProgramUniform1i("texDepth", 3);
            shaderDeferredFirstPerson.setProgramUniform1i("texShadow", 4);
            shaderDeferredFirstPerson.setProgramUniform1i("texLight", 5);
            shaderDeferredFirstPerson.setProgramUniform1i("texBlockLight", 6);
            shaderDeferredFirstPerson.setProgramUniform1i("texAO", 7);
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


    public void initAO(int displayWidth, int displayHeight) {
        boolean enableAO = Game.instance.settings.ao > 0 && Game.instance.getVendor() != GPUVendor.INTEL;
        if (HBAOPlus.hasContext && !enableAO) {
            Engine.checkGLError("pre GLNativeLib.deleteContext");
            HBAOPlus.deleteContext();
            Engine.checkGLError("post GLNativeLib.deleteContext");
            HBAOPlus.hasContext = false;
        }
        if (!HBAOPlus.hasContext && enableAO) {
            updateHBAOSettings();
            Engine.checkGLError("pre GLNativeLib.createContext");
            HBAOPlus.createContext(displayWidth, displayHeight);
            Engine.checkGLError("post GLNativeLib.createContext");
            HBAOPlus.hasContext = true;
        }
        fbSSAO.bind();
        fbSSAO.clearFrameBuffer();
        if (enableAO) {
            HBAOPlus.setDepthTex(Engine.getSceneFB().getDepthTex());
            HBAOPlus.setNormalTex(Engine.getSceneFB().getTexture(1));
            long ptr1 = MemoryUtil.memAddress(Engine.getMatSceneV_YZ_Inv().get());
            HBAOPlus.setViewMatrix(ptr1);
            long ptr = MemoryUtil.memAddress(Engine.getMatSceneP().get());
            HBAOPlus.setProjMatrix(ptr);
            HBAOPlus.setOutputFBO(fbSSAO.getFB());
        }
        Shader.disable();
        FrameBuffer.unbindFramebuffer();
    }
    public void updateHBAOSettings() {
//      HBAOPlus.setRadius(1);
//      HBAOPlus.setBias(0.2f);
//      HBAOPlus.setCoarseAO(1.2f);
//      HBAOPlus.setBlur(true, 8, 16.0f);
//      HBAOPlus.setBlurSharpen(false, 16, 0, 0);
//      HBAOPlus.setDetailAO(1f);
//      HBAOPlus.setPowerExponent(1);
//      HBAOPlus.setDepthThreshold(false, 220, 0.5f);
        HBAOPlus.setRadius(1.8f);
        HBAOPlus.setBias(0.15f);
        HBAOPlus.setCoarseAO(1.3f);
        HBAOPlus.setBlur(true, 8, 16.0f);
        HBAOPlus.setBlurSharpen(false, 16, 0, 0);
        HBAOPlus.setDetailAO(0.9f);
        HBAOPlus.setPowerExponent(1.5f);
        HBAOPlus.setDepthThreshold(false, 220, 0.5f);
        HBAOPlus.setNormalDecodeScaleBias(2.0f,-1f);
        HBAOPlus.setRenderMask(1|2);
        HBAOPlus.setBlur(true, 8, 16);
    }
    public void initAA() {
        if (smaa != null) {
            this.smaa.releaseAll(null);
            smaa = null;
        }
        if (Game.instance.getVendor() != GPUVendor.INTEL && Game.instance.settings.aa > 0) {
            smaa = new SMAA(Game.instance.settings.smaaQuality);
            this.smaa.init(Game.displayWidth, Game.displayHeight);
        }
    }

    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        Engine.checkGLError("releaseAll(EResourceType.FRAMEBUFFER)");
        initAA();
        fbScene = new FrameBuffer(displayWidth, displayHeight);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGBA16F);
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

        GL.deleteTexture(this.preWaterDepthTex);
        this.preWaterDepthTex = GL.genStorage(displayWidth, displayHeight, GL14.GL_DEPTH_COMPONENT32, GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);

        
        
        int blurDownSample = 2;
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

        fbDeferred = FrameBuffer.make(this, displayWidth, displayHeight, GL_RGB16F);
        fbFinal = new FrameBuffer(displayWidth, displayHeight);
        fbFinal.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbFinal.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        fbFinal.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbFinal.setHasDepthAttachment();
        fbFinal.setup(this);
        fbFinal.bind();
        fbFinal.clearFrameBuffer();
        


        fbSSAO = new FrameBuffer(displayWidth, displayHeight);
        fbSSAO.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbSSAO.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
        fbSSAO.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 1F, 1F, 1F);
        fbSSAO.setup(this);
        fbSSAO.bind();
        fbSSAO.clearFrameBuffer();
        

        fbSSR = new FrameBuffer(displayWidth, displayHeight);
        fbSSR.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSR.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSR.setup(this);
        fbSSR.bind();
        fbSSR.clearFrameBuffer();
        
        //TODO: test downsampling ssr blur
        fbSSRBlurredX = new FrameBuffer(displayWidth, displayHeight);
        fbSSRBlurredX.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSRBlurredX.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRBlurredX.setup(this);
        fbSSRBlurredX.bind();
        fbSSRBlurredX.clearFrameBuffer();
        fbSSRBlurredY = new FrameBuffer(displayWidth, displayHeight);
        fbSSRBlurredY.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSRBlurredY.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRBlurredY.setup(this);
        fbSSRBlurredY.bind();
        fbSSRBlurredY.clearFrameBuffer();
        fbSSRCombined = new FrameBuffer(displayWidth, displayHeight);
        fbSSRCombined.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbSSRCombined.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRCombined.setup(this);
        fbSSRCombined.bind();
        fbSSRCombined.clearFrameBuffer();
        
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
        if (this.shaderSSR != null) {
            this.shaderSSR.enable();
            this.shaderSSR.setProgramUniformMatrix4("pixelProj", false, scaleMatBuf, false);
        }
        initAO(displayWidth, displayHeight);
    
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
