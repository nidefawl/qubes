/**
 * 
 */
package nidefawl.qubes.render.post;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.SimpleResourceManager;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class SMAA {

    SimpleResourceManager mgr = new SimpleResourceManager();

    public Shader       shaderAAEdge;
    public Shader       shaderCopyTexture;
    public Shader       shaderDrawAlphaChannel;
    public Shader       shaderAABlendWeight;
    public Shader       shaderAANeighborBlend;
    public Shader       shaderTemporalResolve;
    private FrameBuffer fbFlipInput;
    private FrameBuffer fbAAEdge;
    private FrameBuffer fbAAWeightBlend;
    private int areaTex;
    private int searchTex;

    private boolean srgb;

    private boolean usePredicatedThresholding;

    private boolean useReprojection;

    private FrameBuffer[] fbOutput = new FrameBuffer[2];
    int curBuffer=0;
    int prevBuffer=1;


    public static boolean LOAD_SPIR = false;
    public final static int SMAA_PRESET_LOW = 0;
    public final static int SMAA_PRESET_MEDIUM = 1;
    public final static int SMAA_PRESET_HIGH = 2;
    public final static int SMAA_PRESET_ULTRA = 3;
    final static String[] qualDefines = { "SMAA_PRESET_LOW", "SMAA_PRESET_MEDIUM", "SMAA_PRESET_HIGH", "SMAA_PRESET_ULTRA" };
    public static String[] qualDesc = { "Low", "Medium", "High", "Ultra" };
    public SMAA(final int quality, int w, int h) {
        this(quality, false, false, false, w, h);
    }
    public SMAA(final int quality, final boolean usePredTex, boolean srgb, final boolean reprojection, final int w, final int h) {
        this.useReprojection = reprojection;
        this.srgb = srgb;
        this.usePredicatedThresholding = usePredTex;
        Engine.TEMPORAL_OFFSET = useReprojection;
        AssetManager assetMgr = AssetManager.getInstance();
        AssetBinary areaTexData = assetMgr.loadBin("textures/areatex.bin");
        AssetBinary searchTexData = assetMgr.loadBin("textures/searchtex.bin");
        areaTex = makeAATexture(areaTexData.getData(), areaTex, 160, 560, 320, GL_RG8);
        searchTex = makeAATexture(searchTexData.getData(), searchTex, 64, 16, 64, GL_R8);
        IShaderDef def = new IShaderDef() {
            @Override
            public String getDefinition(String define) {
                if ("SMAA_QUALITY".equals(define)) {
                    return "#define "+qualDefines[quality];
                }
                if ("SMAA_PREDICATION".equals(define)) {
                    return "#define SMAA_PREDICATION "+(usePredTex?"1":"0");
                }
                if ("SMAA_REPROJECTION".equals(define)) {
                    return "#define SMAA_REPROJECTION "+(reprojection?"1":"0");
                }
                if ("SMAA_RT_METRICS".equals(define)) {
                    return "#define SMAA_RT_METRICS float4(1.0 / "+w+".0, 1.0 / "+h+".0, "+w+".0, "+h+".0)";
                }
                return null;
            }
        };
        

        try {
            Shader new_CopyTexture;
            Shader new_AAEdge;
            Shader new_BlendWeight;
            Shader new_neighbor_blend;
            Shader new_temporal_resolve = null;
            shaderDrawAlphaChannel = assetMgr.loadShader(mgr, "debug/drawAlphaChannel", "screen_scaled_quad", null, null, def);
            if (LOAD_SPIR) {
                new_CopyTexture = assetMgr.loadShaderBinary(mgr, "spir/copytexture_frag.spv", "spir/copytexture_vert.spv", def);
                new_AAEdge = assetMgr.loadShaderBinary(mgr, "spir/SMAA_edge_frag.spv", "spir/SMAA_edge_vert.spv", def);
                new_BlendWeight = assetMgr.loadShaderBinary(mgr, "spir/SMAA_blend_weight_frag.spv", "spir/SMAA_blend_weight_vert.spv", def);
                new_neighbor_blend = assetMgr.loadShaderBinary(mgr, "spir/SMAA_neighbour_blend_frag.spv", "spir/SMAA_neighbour_blend_vert.spv", def);
            } else {
                new_CopyTexture = assetMgr.loadShader(mgr, "post/SMAA/copytexture", def);
                new_AAEdge = assetMgr.loadShader(mgr, "post/SMAA/SMAA_edgedetection", def);
                new_BlendWeight = assetMgr.loadShader(mgr, "post/SMAA/SMAA_blend_weight", def);
                new_neighbor_blend = assetMgr.loadShader(mgr, "post/SMAA/SMAA_neighbor_blend", def);
            }
            if (useReprojection) {
                new_temporal_resolve = assetMgr.loadShader(mgr, "post/SMAA/SMAA_temporal_resolve", def);
            }
            shaderCopyTexture = new_CopyTexture;
            shaderAAEdge = new_AAEdge;
            shaderAABlendWeight = new_BlendWeight;
            shaderAANeighborBlend = new_neighbor_blend;
            shaderTemporalResolve = new_temporal_resolve;
            shaderCopyTexture.enable();
            shaderCopyTexture.setProgramUniform1i("texColor", 0);
            if (usePredTex) {
                shaderCopyTexture.setProgramUniform1i("texMaterial", 1);
            }
            if (useReprojection) {
                shaderCopyTexture.setProgramUniform1i("velocityTex", 2);
            }
            shaderAAEdge.enable();
            shaderAAEdge.setProgramUniform1i("texColor", 0);
            if (usePredTex) {
                shaderAAEdge.setProgramUniform1i("texMaterial", 1);    
            }
            shaderAABlendWeight.enable();
            shaderAABlendWeight.setProgramUniform1i("edgesTex", 0);
            shaderAABlendWeight.setProgramUniform1i("areaTex", 1);
            shaderAABlendWeight.setProgramUniform1i("searchTex", 2);
            shaderAABlendWeight.setProgramUniform4f("jitterOffset", 0, 0, 0, 0);
            shaderAANeighborBlend.enable();
            shaderAANeighborBlend.setProgramUniform1i("texColor", 0);
            shaderAANeighborBlend.setProgramUniform1i("blendTex", 1);
            shaderAANeighborBlend.setProgramUniform1i("velocityTex", 2);
            if (useReprojection) {
                shaderTemporalResolve.enable();
                shaderTemporalResolve.setProgramUniform1i("texColorCurrent", 0);
                shaderTemporalResolve.setProgramUniform1i("texColorPrev", 1);
                shaderTemporalResolve.setProgramUniform1i("velocityTex", 2);
            }
            Shader.disable();
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            throw new GameError(e);
        }
        init(w, h);
    }
    
    void init(int displayWidth, int displayHeight) {
//        this.fbFlipInput = FrameBuffer.make(mgr, displayWidth, displayHeight, GL_RGBA8);
        int format = srgb?GL21.GL_SRGB8_ALPHA8:GL_RGBA8;
        this.fbFlipInput = new FrameBuffer(displayWidth, displayHeight);
        this.fbFlipInput.setColorAtt(GL_COLOR_ATTACHMENT0, format);
        this.fbFlipInput.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        this.fbFlipInput.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        this.fbFlipInput.setColorAtt(GL_COLOR_ATTACHMENT1, GL_R16F);
        this.fbFlipInput.setFilter(GL_COLOR_ATTACHMENT1, GL_LINEAR, GL_LINEAR);
        this.fbFlipInput.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
        this.fbFlipInput.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RG16F);
        this.fbFlipInput.setFilter(GL_COLOR_ATTACHMENT2, GL_LINEAR, GL_LINEAR);
        this.fbFlipInput.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        this.fbFlipInput.setup(mgr);
        this.fbAAEdge = FrameBuffer.make(mgr, displayWidth, displayHeight, format, true, true);
        this.fbAAWeightBlend = FrameBuffer.make(mgr, displayWidth, displayHeight, format, true, true);
        if (this.useReprojection) {
            for (int i = 0; i < 2; i++) {
                FrameBuffer fb = new FrameBuffer(displayWidth, displayHeight);
                fb.setColorAtt(GL_COLOR_ATTACHMENT0, format);
                fb.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
                fb.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
                fb.setup(mgr);
                this.fbOutput[i] = fb;
            }
        }
        FrameBuffer.unbindFramebuffer();
    }
    

    private int makeAATexture(byte[] dataIn, int i, int w, int h, int stride, int format) {
        if (i<=0) {
            i = GL11.glGenTextures();
        }
        if (format == GL_RG8 && w*h*2 != dataIn.length) {
            throw new GameError("Invalid SMAA area texture");
        }
        if (format == GL_R8 && w*h != dataIn.length) {
            throw new GameError("Invalid SMAA search texture");
        }

        byte[] data =  dataIn;
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, i);
        glTexParameteri(GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        Engine.checkGLError("glTexParameteri");
        ByteBuffer directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        directBuf.clear();
        directBuf.put(data, 0, data.length);
        directBuf.position(0).limit(data.length);
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format == GL_R8 ? GL_RED : GL_RG, GL11.GL_UNSIGNED_BYTE, directBuf);
        Engine.checkGLError("GL11.glTexImage2D");
        return i;
    }
    
    public void releaseAll(EResourceType t) {
        if (t == null) this.mgr.release();
        else this.mgr.releaseAll(t);
    }

    /**
     * @param texture
     * @param finalTarget 
     */
    public void render(int texture, int material, int velocity, int debugTexture, FrameBuffer finalTarget) {
        if (true||Game.GL_ERROR_CHECKS) {
            boolean b = glGetBoolean(GL_DEPTH_TEST);
            if (!b) {
                System.err.println("NEED DEPTH TESTING!");
            }
            if (Engine.isBlend()) {
                System.err.println("NEED disabled blend!");
            }
            b = glGetBoolean(GL_DEPTH_WRITEMASK);
            if (!b) {
                System.err.println("NEED GL_DEPTH_WRITEMASK for discard!");
            }
            int i = glGetInteger(GL_DEPTH_FUNC);
            if (i != (Engine.isInverseZ?GL_GEQUAL:GL_LEQUAL)) {
                System.err.println("GL_DEPTH_FUNC != "+(Engine.isInverseZ?"GL_GEQUAL":"GL_LEQUAL")+", CHECK STATES");
            }
           
        }
        
//        Engine.updateOrthoMatrix(Game.displayWidth, Game.displayHeight, true);
//        UniformBuffer.updateOrtho();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("fbFlipInput");
        fbFlipInput.bind();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        if (this.usePredicatedThresholding) {
            GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, material);
        }
        if (this.useReprojection) {
            GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, velocity);
        }
        shaderCopyTexture.enable();
        Engine.drawFSTri();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("renderSMAA");
        if (debugTexture != 1) {
            renderSMAA(fbFlipInput.getTexture(0), usePredicatedThresholding?fbFlipInput.getTexture(1):0, useReprojection?fbFlipInput.getTexture(2):0, velocity, debugTexture, finalTarget);
        }
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
//        Engine.updateOrthoMatrix(Game.displayWidth, Game.displayHeight, false);
//        UniformBuffer.updateOrtho();
        if (debugTexture > 0) {
            if (finalTarget == null) {
                FrameBuffer.unbindFramebuffer();
            } else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
            int tex = 0;
            switch (debugTexture) {
                case 1:
                    Shaders.textured.enable();
                    tex = fbFlipInput.getTexture(0);
                    break;
                case 2:
                    Shaders.textured_to_srgb.enable();
                    tex = fbAAEdge.getTexture(0);
                    break;
                case 3:
                    Shaders.textured.enable();
//                    shaderDrawAlphaChannel.enable();
//                    shaderDrawAlphaChannel.setProgramUniform1i("texColor", 0);
                    tex = fbAAWeightBlend.getTexture(0);
                    break;
                case 4:
                    Shaders.textured.enable();
                    tex = fbFlipInput.getTexture(2);
                    break;
            }
            glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, tex);
            
            Engine.drawFSQuad(1);
        }
    }
    public void renderSMAA(int texture, int material, int velocityInverted, int velocity, int debugTexture, FrameBuffer finalTarget) {
        if (useReprojection) {
            prevBuffer = curBuffer;
            curBuffer = (curBuffer+1) % 2;
        }
        
        fbAAEdge.bind();
        fbAAEdge.clearFrameBuffer();
        shaderAAEdge.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        if (this.usePredicatedThresholding) {
            GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, material);
        }
        Engine.drawFSTri();
        if (debugTexture == 1) {
            return;
        }
        fbAAWeightBlend.bind();
        fbAAWeightBlend.clearFrameBuffer();
        //copy over the depth buffer
        fbAAEdge.bindRead();
        GL30.glBlitFramebuffer(0, 0, fbAAEdge.getWidth(), fbAAEdge.getHeight(), 0, 0, fbAAWeightBlend.getWidth(), fbAAWeightBlend.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
        
        shaderAABlendWeight.enable();
        if (useReprojection) {
            int n = Engine.getTemporalJitterIdx();
            if (n%2==0)
                shaderAABlendWeight.setProgramUniform4f("jitterOffset", 1, 1, 1, 0);
            else 
                shaderAABlendWeight.setProgramUniform4f("jitterOffset", 2, 2, 2, 0);
        } else {

//            shaderAABlendWeight.setProgramUniform4f("jitterOffset", 0, 0, 0, 0);
        }
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbAAEdge.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.areaTex);
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, this.searchTex);

        Engine.setDepthFunc(GL_EQUAL); // only draw equal z fragments, +30% speed
        Engine.drawFSTri();
        Engine.setDepthFunc(GL_LEQUAL);
        if (debugTexture == 2) {
            return;
        }
        if (useReprojection) {
            this.fbOutput[curBuffer].bind();
        } else {
            if (finalTarget == null) FrameBuffer.unbindFramebuffer();
            else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
        }
        shaderAANeighborBlend.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, fbAAWeightBlend.getTexture(0));
        if (this.useReprojection) {
            GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, velocityInverted);
        }
        Engine.drawFSTri();
        if (useReprojection) {
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fbOutput[curBuffer].getTexture(0));
            GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.fbOutput[prevBuffer].getTexture(0));
            if (this.useReprojection) {
                GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, velocity);
            }
            shaderTemporalResolve.enable();
            if (finalTarget == null) FrameBuffer.unbindFramebuffer();
            else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
            Engine.drawFSTri();
        } 
    }


}
