package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.List;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.FinalRenderer;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;

public class FinalRendererGL extends FinalRenderer {

    public Shader       shaderBloomCombine;
    public Shader       shaderFinal;
    public Shader       shaderDeferred;
    public Shader       shaderDeferredWater;
    public Shader       shaderDeferredFirstPerson;
    public Shader       shaderInterpLum;
    public Shader       shaderThreshold;
    public Shader       shaderSSR;
    public Shader       shaderSSRCombine;
    public Shader       shaderDownsample4x;
    public Shader       shaderDownsample4xLum;
    private Shader shaderNormals;

    public FrameBuffer  fbSSR;
    public FrameBuffer  fbSSRCombined;
    public FrameBuffer  fbDeferred;
    public FrameBuffer  fbSSAO;
    public FrameBuffer  fbBloomOut;
    private FrameBuffer fbLuminanceDownsample[];
    private FrameBuffer fbLuminanceInterp[];
    public FrameBuffer  fbTonemappedDepth;

    private int preWaterDepthTex;
    private boolean     startup     = true;
    SMAA smaa;
    public boolean aoNeedsInit = false;
    int texSlotNoise = 0;
    private int         ssr         = 0;
    float               curBrightness;
    float               brightness;
    private int         frame;

    public void renderDeferred(float fTime, int pass) {
        this.fbDeferred.bind();
        if (pass == 0) {
            this.fbDeferred.clearDepth();
        }
        int drawFlags = 1;
        if (Engine.getRenderMaterialBuffer())
            drawFlags |= (1<<getAttPointMaterial());
        if (Engine.getRenderVelocityBuffer()&&pass<3)
            drawFlags |= (1<<getAttPointVelocity());
        this.fbDeferred.setDrawMask(drawFlags);
        
        texSlotNoise++;
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
//        System.out.println(GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.getShadowDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getLightTexture());
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
        if (pass == 1) {
            GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, this.preWaterDepthTex);
            GL.bindTexture(GL_TEXTURE8, GL_TEXTURE_2D, TMgr.getNoise());
        } else if (pass == 0) {
            GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, Engine.getAOTexture());
        }
        GL.bindTexture(GL_TEXTURE9, GL_TEXTURE_2D_ARRAY, TMgr.getNoiseArr());
        
        int slot = 0;
        if (TextureArrays.noiseTextureArray.getNumTextures() > 0) {
            slot = texSlotNoise%TextureArrays.noiseTextureArray.getNumTextures();
        }
        shaderDeferred.setProgramUniform1i("texSlotNoise", slot);
        if (Engine.getRenderVelocityBuffer() ) {
            shaderDeferred.setProgramUniformMatrix4("mat_reproject", false, Engine.getMatReproject().get(), false);    
        }
        

        Engine.drawFSTri();

        if (drawFlags != 1) {
            this.fbDeferred.setDrawMask(drawFlags);
        }
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
            GLDebugTextures.readTexture(false, name, "texColor", Engine.getSceneFB().getTexture(0));
            GLDebugTextures.readTexture(false, name, "texNormals", Engine.getSceneFB().getTexture(1));
            GLDebugTextures.readTexture(false, name, "texMaterial", Engine.getSceneFB().getTexture(2));
            GLDebugTextures.readTexture(false, name, "blocklight", Engine.getSceneFB().getTexture(3));
            GLDebugTextures.readTexture(false, name, "texDepth", Engine.getSceneFB().getDepthTex(), 2);
            GLDebugTextures.readTexture(true, name, "DeferredOutColor", this.fbDeferred.getTexture(0), 8);
            if (Engine.getRenderMaterialBuffer())
            GLDebugTextures.readTexture(true, name, "MaterialBuffer", this.fbDeferred.getTexture(getAttPointMaterial()));
            if (Engine.getRenderVelocityBuffer())
                GLDebugTextures.readTexture(true, name, "VelocityBuffer", this.fbDeferred.getTexture(getAttPointVelocity()));
            if (Engine.lightCompute != null)
            GLDebugTextures.readTexture(false, name, "light", RenderersGL.lightCompute.getTexture());
            if (pass == 0) {
                GLDebugTextures.readTexture(false, name, "texShadow", Engine.getShadowDepthTex(), 2);
                GLDebugTextures.readTexture(false, name, "texShadowDbg", ((ShadowRendererGL) Engine.shadowRenderer).getDebugTexture());
                GLDebugTextures.readTexture(false, name, "AOOut", Engine.getAOTexture());
            }
            if (pass == 1) {
                GLDebugTextures.readTexture(false, name, "preWaterDepth", this.preWaterDepthTex, 2);
            }
        }

    }
    
    public void calcLum() {
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Luminance");
        FrameBuffer inputBuffer = ssr > 0 ? this.fbSSRCombined : this.fbDeferred;
        FrameBuffer top = this.fbLuminanceDownsample[0];
        
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, inputBuffer.getTexture(0)); // Albedo
        Engine.setViewport(0, 0, top.getWidth(), top.getHeight()); // 4x downsampled render resolutino
        
        float twoPixelX = 2.0f/(float)inputBuffer.getWidth();
        float twoPixelY = 2.0f/(float)inputBuffer.getHeight();
        shaderDownsample4xLum.enable();
        shaderDownsample4xLum.setProgramUniform2f("twoTexelSize", twoPixelX, twoPixelY);
        top.bind();
        top.clearFrameBuffer();
        Engine.drawFSTri();
        shaderDownsample4x.enable();
        
        FrameBuffer lastBound = this.fbLuminanceDownsample[0];
        for (int i = 0; i < this.fbLuminanceDownsample.length-1; i++) {
            
//            System.out.println("input w/h "+this.fbLuminanceDownsample[i].getWidth()+", "+this.fbLuminanceDownsample[i].getHeight());
            
            twoPixelX = 2.0f / (float) this.fbLuminanceDownsample[i].getWidth();
            twoPixelY = 2.0f / (float) this.fbLuminanceDownsample[i].getHeight();
            lastBound = this.fbLuminanceDownsample[i+1];

            Engine.setViewport(0, 0, lastBound.getWidth(), lastBound.getHeight()); // 16x downsampled render resolutino
            
            shaderDownsample4x.setProgramUniform2f("twoTexelSize", twoPixelX, twoPixelY);

            lastBound.bind();
            lastBound.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceDownsample[i].getTexture(0));
            Engine.drawFSTri();

        }
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        this.fbLuminanceInterp[indexOut].bind();
        this.fbLuminanceInterp[indexOut].clearFrameBuffer();
        this.shaderInterpLum.enable();
        this.shaderInterpLum.setProgramUniform1f("elapsedTime", (Stats.avgFrameTime)/100f);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbLuminanceInterp[indexIn].getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, lastBound.getTexture(0));
        Engine.drawFSTri();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(false, "Luminance", "inputBuffer", inputBuffer.getTexture(0), 1);
            GLDebugTextures.readTexture(true, "Luminance", "Output", this.fbLuminanceInterp[indexOut].getTexture(0), 1);
        }

        Engine.setDefaultViewport();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
    }

    



    public void render(World world, float fTime, int pass) {
        renderDeferred(fTime, pass);
        
    }
    public void renderBlur() {
        calcLum();
    }

    public int getSsr() {
        return this.ssr;
    }
    /**
     * @param world
     * @param fTime
     */
    public void raytraceSSR() {
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("raytrace");
        Engine.setViewport(0, 0, fbSSR.getWidth(), fbSSR.getHeight());
        fbSSR.bind();
        fbSSR.clearFrameBuffer();
        shaderSSR.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0)); //COLOR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1)); //NORMAL
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2)); //MATERIAL
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, this.fbBloomOut.getDepthTex()); //DEPTH
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_CUBE_MAP, RenderersGL.skyRenderer.fbSkybox.getTexture(0));//SKYBOX CUBEMAP
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, this.preWaterDepthTex); //DEPTH PreWater
        Engine.drawFSTri();
        Engine.setDefaultViewport();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
    }

    /**
     * @param world
     * @param fTime
     */
    public void combineSSR() {
      
        
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("blur");
        int blurred = RenderersGL.blurRenderer.renderBlurSeperate(fbSSR.getTexture(0), 8);
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        
        
        
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("combine");
        shaderSSRCombine.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0)); //COLOR
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurred); //Blurred
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2)); //MATERIAL
        fbSSRCombined.bind();
        fbSSRCombined.clearFrameBuffer();
        Engine.drawFSTri();
        Shader.disable();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();

  
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(false, "SSR", "DeferredInput", fbDeferred.getTexture(0), 1);
            GLDebugTextures.readTexture(true, "SSR", "SSROutput", fbSSR.getTexture(0), 8);
            GLDebugTextures.readTexture(true, "SSR", "SSRBlurred", blurred, 8);
            GLDebugTextures.readTexture(true, "SSR", "SSRBlurCombined", fbSSRCombined.getTexture(0), 8);
            GLDebugTextures.readTexture(false, "SSR", "texDepth", this.fbBloomOut.getDepthTex(), 2);
            GLDebugTextures.readTexture(false, "SSR", "texColor", fbDeferred.getTexture(0));
            GLDebugTextures.readTexture(false, "SSR", "texNormals", Engine.getSceneFB().getTexture(1));
            GLDebugTextures.readTexture(false, "SSR", "texMaterial", Engine.getSceneFB().getTexture(2));
        }
    }


    public void renderAA(int inputTexture, FrameBuffer output, boolean isWorld) {
        if (smaa != null) {
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("SMAA");
            this.smaa.render(inputTexture, 
                    Engine.getRenderMaterialBuffer()&&isWorld?fbDeferred.getTexture(getAttPointMaterial()):TMgr.getEmpty(), 
                    Engine.getRenderVelocityBuffer()&&isWorld?fbDeferred.getTexture(getAttPointVelocity()):TMgr.getEmpty(), 
                            0, output);
            if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        } else {
            if (output != null) output.bindAndClear();
            else FrameBuffer.unbindFramebuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, inputTexture);
            Shaders.textured.enable();
            Engine.drawFullscreenQuad();
        }
    }
    public FrameBuffer renderTonemap() {
        this.frame++;
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbBloomOut.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.fbLuminanceInterp[indexOut].getTexture(0));
        fbTonemappedDepth.bind();
//        fbTonemappedDepth.setDrawMask(1);
        shaderFinal.enable();
        Engine.drawFSTri();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(false, "Tonemap", "texColor", fbBloomOut.getTexture(0), 1);
            GLDebugTextures.readTexture(false, "Tonemap", "LuminanceInterp", this.fbLuminanceInterp[indexOut].getTexture(0), 1);
            GLDebugTextures.readTexture(true, "Tonemap", "Output", fbDeferred.getTexture(0));
        }
        return fbTonemappedDepth;
    }
    
    public int renderNormals() {
        fbBloomOut.bind();
        fbBloomOut.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        this.shaderNormals.enable();
        Engine.drawFSTri();
        GLDebugTextures.readTexture(false, "fixNormals", "texNormals", Engine.getSceneFB().getTexture(1), 8);
        return GLDebugTextures.readTexture(true, "fixNormals", "output", fbBloomOut.getTexture(0), 8);
    }
    public void copyPreWaterDepth() {
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.preWaterDepthTex);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("bindTexture "+this.preWaterDepthTex);
        ARBCopyImage.glCopyImageSubData(Engine.getSceneFB().getDepthTex(), GL_TEXTURE_2D, 0, 0, 0, 0, this.preWaterDepthTex, GL_TEXTURE_2D, 0, 0, 0, 0, Engine.getSceneFB().getWidth(), Engine.getSceneFB().getHeight(), 1);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glCopyImageSubData "+Engine.getSceneFB().getDepthTex()+" -> "+this.preWaterDepthTex);
        
    }
    
    /**
     * Store scene depth in fbBloomOut
     * We need it later  
     */
    public void copySceneDepthBuffer() {
        fbTonemappedDepth.bind();

        Engine.getSceneFB().bindRead();
        //             
        GL30.glBlitFramebuffer(0, 0, Engine.getSceneFB().getWidth(), Engine.getSceneFB().getHeight(), 0, 0, fbTonemappedDepth.getWidth(), fbTonemappedDepth.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
    }
    public void renderBloom() {
        FrameBuffer input = ssr > 0 ? fbSSRCombined : fbDeferred;
        FrameBuffer output = ssr > 0 ? fbDeferred : fbSSRCombined;

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Threshold");
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input.getTexture(0)); // Albedo
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3)); // blocklight
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getLightTexture());
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(false, "Bloom", "texColor", input.getTexture(0), 1);
            GLDebugTextures.readTexture(false, "Bloom", "blocklight", Engine.getSceneFB().getTexture(3));
            GLDebugTextures.readTexture(false, "Bloom", "lightComputed", Engine.getLightTexture());
        }
        shaderThreshold.enable();
        output.bind();
        output.setDrawMask(1);
        glClear(GL_COLOR_BUFFER_BIT);

        Engine.drawFSTri();
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(true, "Bloom", "brightPixels", output.getTexture(0), 1);
        }

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Blur");
        int blurTexture = RenderersGL.blurRenderer.renderBlur1PassDownsample(output.getTexture(0));
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(true, "Bloom", "blurredTexture", blurTexture);
        }
        

        fbBloomOut.bind();
//        fbFinal.clearFrameBuffer();
        shaderBloomCombine.enable();
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("enable shaderBlur");

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, blurTexture);

        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("Combine");

        Engine.drawFSTri();
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(true, "Bloom", "bloomCombined", fbBloomOut.getTexture(0));
        }
        if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
    }

    static final class DeferredDefs implements IGraphicsShaderDef {
        private final int pass;
        private FinalRendererGL fr;
        public DeferredDefs(FinalRendererGL fr, int pass) {
            this.fr = fr;
            this.pass = pass;
        }
        @Override
        public String getDefinition(String define) {
            if ("RENDER_PASS".equals(define)) {
                return "#define RENDER_PASS "+pass;
            }
            if ("RENDER_AMBIENT_OCCLUSION".equals(define)) {
                return "#define RENDER_AMBIENT_OCCLUSION "+(Engine.RENDER_SETTINGS.ao%2);
            }
            if ("RENDER_VELOCITY_BUFFER".equals(define)) {
                return "#define RENDER_VELOCITY_BUFFER "+(Engine.getRenderVelocityBuffer() ? "1" : "0");
//                return "#define RENDER_VELOCITY_BUFFER 1"; 
            }
            if ("RENDER_MATERIAL_BUFFER".equals(define)) {
                return "#define RENDER_MATERIAL_BUFFER "+(Engine.getRenderMaterialBuffer() ? "1" : "0"); 
//                return "#define RENDER_MATERIAL_BUFFER 1"; 
            }
            if ("BLUE_NOISE".equals(define)) {
                return "#define BLUE_NOISE"; 
            }
            return null;
        }
        @Override
        public void bindFragDataLocations(int shader) {
            GL30.glBindFragDataLocation(shader, 0, "out_Color");
            if (Engine.getRenderVelocityBuffer()) {
                GL30.glBindFragDataLocation(shader, this.fr.getAttPointVelocity(), "out_Velocity");
            }
            if (Engine.getRenderMaterialBuffer()) {
                GL30.glBindFragDataLocation(shader, this.fr.getAttPointMaterial(), "out_FinalMaterial");
            }
        }
    }

    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderSSR = assetMgr.loadShader(this, "post/SSR/ssr", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SSR".equals(define)) {
                        int ssr = Engine.RENDER_SETTINGS.ssr;
                        return "#define SSR_"+(ssr<1?1:ssr>3?3:ssr);
                    }
                    return null;
                }
            });
            Shader new_shaderSSRCombine = assetMgr.loadShader(this, "post/SSR/ssr_combine");
            Shader new_shaderDef = assetMgr.loadShader(this, "post/deferred", new DeferredDefs(this, 0));
            Shader new_shaderDef2 = assetMgr.loadShader(this, "post/deferred", new DeferredDefs(this, 1));
            Shader new_shaderDef3 = assetMgr.loadShader(this, "post/deferred", new DeferredDefs(this, 2));
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
            shaderBloomCombine = new_shaderbloom_combine;
            shaderFinal = new_shaderFinal;
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
            shaderSSR.setProgramUniform1i("texSkybox", 4);
            shaderSSR.setProgramUniform1i("texDepthPreWater", 5);
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
            shaderDeferred.setProgramUniform1i("texArrayNoise", 9);
            shaderDeferredWater.enable();
            shaderDeferredWater.setProgramUniform1i("texColor", 0);
            shaderDeferredWater.setProgramUniform1i("texNormals", 1);
            shaderDeferredWater.setProgramUniform1i("texMaterial", 2);
            shaderDeferredWater.setProgramUniform1i("texDepth", 3);
            shaderDeferredWater.setProgramUniform1i("texShadow", 4);
            shaderDeferredWater.setProgramUniform1i("texLight", 5);
            shaderDeferredWater.setProgramUniform1i("texBlockLight", 6);
            shaderDeferredWater.setProgramUniform1i("texDepthPreWater", 7);
            shaderDeferredWater.setProgramUniform1i("texWaterNoise", 8);
            shaderDeferredWater.setProgramUniform1i("texArrayNoise", 9);
            shaderDeferredFirstPerson.enable();
            shaderDeferredFirstPerson.setProgramUniform1i("texColor", 0);
            shaderDeferredFirstPerson.setProgramUniform1i("texNormals", 1);
            shaderDeferredFirstPerson.setProgramUniform1i("texMaterial", 2);
            shaderDeferredFirstPerson.setProgramUniform1i("texDepth", 3);
            shaderDeferredFirstPerson.setProgramUniform1i("texShadow", 4);
            shaderDeferredFirstPerson.setProgramUniform1i("texLight", 5);
            shaderDeferredFirstPerson.setProgramUniform1i("texBlockLight", 6);
            shaderDeferredFirstPerson.setProgramUniform1i("texAO", 7);
            shaderDeferredFirstPerson.setProgramUniform1i("texArrayNoise", 9);
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

    int[] aoSize;

    public void initAO() {
        boolean enableAO = HBAOPlus.available && Engine.RENDER_SETTINGS.ao > 0 && GameBase.baseInstance.getVendor() != GPUVendor.INTEL;
        if (HBAOPlus.hasContext && !enableAO) {
            Engine.checkGLError("pre GLNativeLib.deleteContext");
            HBAOPlus.deleteContext();
            Engine.checkGLError("post GLNativeLib.deleteContext");
            HBAOPlus.hasContext = false;
        }
        if (!HBAOPlus.hasContext && enableAO) {
            updateHBAOSettings();
            Engine.checkGLError("pre GLNativeLib.createContext");
            HBAOPlus.createContext(128, 128, GameBase.baseInstance.caps);
//            HBAOPlus.createContext(Game.displayWidth, Game.displayHeight, null);
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
        }
        Engine.TEMPORAL_OFFSET = false;
        smaa = null;
        if (GameBase.baseInstance.getVendor() != GPUVendor.INTEL && Engine.RENDER_SETTINGS.smaaMode > 0) {
            smaa = new SMAA(Engine.RENDER_SETTINGS.smaaQuality, Engine.RENDER_SETTINGS.smaaPredication, false, Engine.RENDER_SETTINGS.smaaMode==2);
            this.smaa.init(this.rendererWidth, this.rendererHeight);
        }
    }

    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        Engine.checkGLError("releaseAll(EResourceType.FRAMEBUFFER)");
        initAA();

        GL.deleteTexture(this.preWaterDepthTex);
        this.preWaterDepthTex = GL.genStorage(displayWidth, displayHeight, GL14.GL_DEPTH_COMPONENT32, GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);


        int[] lumSize = GameMath.downsample(displayWidth, displayHeight, 2);

        int lumW = lumSize[0];
        int lumH = lumSize[1];
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
        makeSceneBuffer(displayWidth, displayHeight);
        makeDeferredBuffer(displayWidth, displayHeight);

        
        fbTonemappedDepth = new FrameBuffer(displayWidth, displayHeight);
        fbTonemappedDepth.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbTonemappedDepth.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        fbTonemappedDepth.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbTonemappedDepth.setHasDepthAttachment();
        fbTonemappedDepth.setup(this);
        
        fbBloomOut = new FrameBuffer(displayWidth, displayHeight);
        fbBloomOut.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbBloomOut.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        fbBloomOut.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbBloomOut.setHasDepthAttachment();
        fbBloomOut.setup(this);
        
        this.aoSize = GameMath.downsample(displayWidth, displayHeight, 1);

        fbSSAO = new FrameBuffer(this.aoSize[0], this.aoSize[1]);
        fbSSAO.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbSSAO.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
        fbSSAO.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 1F, 1F, 1F);
        fbSSAO.setup(this);
        int[] ssrSize = GameMath.downsample(displayWidth, displayHeight, this.ssr>2?1:2);
        fbSSR = new FrameBuffer(ssrSize[0], ssrSize[1]);
        fbSSR.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbSSR.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSR.setup(this);
        
        fbSSRCombined = new FrameBuffer(displayWidth, displayHeight);
        fbSSRCombined.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbSSRCombined.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbSSRCombined.setup(this);
        
        aoNeedsInit = true;
        aoReinit();
    }
    
    private void makeDeferredBuffer(int displayWidth, int displayHeight) {
        if (fbDeferred != null) fbDeferred.destroy();
        fbDeferred = new FrameBuffer(displayWidth, displayHeight);
        fbDeferred.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
        fbDeferred.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        fbDeferred.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        if (Engine.getRenderMaterialBuffer())  {
            int attPoint = GL_COLOR_ATTACHMENT0+getAttPointMaterial();
            fbDeferred.setColorAtt(attPoint, GL_R16F);
            fbDeferred.setFilter(attPoint, GL_NEAREST, GL_NEAREST);
            fbDeferred.setClearColor(attPoint, 0F, 0F, 0F, 0F);
            
        }
        if (Engine.getRenderVelocityBuffer())  {
            int attPoint = GL_COLOR_ATTACHMENT0+getAttPointVelocity();
            fbDeferred.setColorAtt(attPoint, GL_RG16F);
            fbDeferred.setFilter(attPoint, GL_NEAREST, GL_NEAREST);
            fbDeferred.setClearColor(attPoint, 0F, 1F, 0F, 0F);
        }
        fbDeferred.setup(this);
    }

    private int getAttPointMaterial() {
        return 1;
    }
    private int getAttPointVelocity() {
        return Engine.getRenderMaterialBuffer() ? 2 : 1;
    }

    private void makeSceneBuffer(int displayWidth, int displayHeight) {
        FrameBuffer prev = Engine.getSceneFB();
        if (prev != null) prev.destroy();
        FrameBuffer fbScene = new FrameBuffer(displayWidth, displayHeight);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGBA16F);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
        fbScene.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
        fbScene.setFilter(GL_COLOR_ATTACHMENT1, GL_NEAREST, GL_NEAREST);
        fbScene.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        fbScene.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
        fbScene.setHasDepthAttachment();
        fbScene.setup(this);
        Engine.setSceneFB(fbScene);
    }

    public void aoReinit() {
        if (aoNeedsInit) {
            aoNeedsInit=false;
            initAO();
        }
    }


    public void release() {
        super.release();
        Engine.setSceneFB(null);
    }



    public void init() {
        initShaders();
        setSSR(Engine.RENDER_SETTINGS.ssr);
        aoNeedsInit = true;
    }
    /**
     * @param id
     */
    public void setSSR(int ssr) {
        this.ssr = ssr;
        initShaders();
    }

    public void onAASettingChanged() {
        RenderersGL.outRenderer.initAA();
        RenderersGL.outRenderer.initShaders();
        makeDeferredBuffer(rendererWidth, rendererHeight);
        makeSceneBuffer(rendererWidth, rendererHeight);
    }

    @Override
    public void onAOSettingUpdated() {
        initAO();
        initShaders();
    }

    @Override
    public void onVRModeChanged() {
        initShaders();
    }
}
