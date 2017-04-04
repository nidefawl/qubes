/**
 * 
 */
package nidefawl.qubes.vulkan;

import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.GL_RG8;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.vulkan.*;

/**
 * @author Michael Hept 2017 Copyright: Michael Hept
 */
public class VkSMAA {
    public final static int SMAA_PRESET_LOW = 0;
    public final static int SMAA_PRESET_MEDIUM = 1;
    public final static int SMAA_PRESET_HIGH = 2;
    public final static int SMAA_PRESET_ULTRA = 3;
    final static String[] qualDefines = { "SMAA_PRESET_LOW", "SMAA_PRESET_MEDIUM", "SMAA_PRESET_HIGH", "SMAA_PRESET_ULTRA" };
    public static String[] qualDesc = { "Low", "Medium", "High", "Ultra" };

    public static VkPipelineLayout pipelineLayoutEdgeDetect = new VkPipelineLayout("pipelineLayoutEdgeDetect");
    public static VkPipelineLayout pipelineLayoutBlendWeight = new VkPipelineLayout("pipelineLayoutBlendWeight");
    public static VkPipelineLayout pipelineLayoutNeighborBlend = new VkPipelineLayout("pipelineLayoutNeighborBlend");
    private boolean initTex = false;
    private static VkTexture areaTex;
    private static VkTexture searchTex;
    public VkPipelineGraphics edgeDetect = new VkPipelineGraphics(pipelineLayoutEdgeDetect);
    public VkPipelineGraphics blendWeight = new VkPipelineGraphics(pipelineLayoutBlendWeight);
    public VkPipelineGraphics neighborBlend = new VkPipelineGraphics(pipelineLayoutNeighborBlend);
    private VkDescriptor descBlendWeight;
    private VkDescriptor descNeighborBlend;
    private FrameBuffer fbedgeDetect;
    private FrameBuffer fbBlendWeight;
    private FrameBuffer fbOutput;
    public VkDescriptor descOutput;
    public VkDescriptor descOutputEdges;
    public VkDescriptor descOutputWeights;

    void initTextures(VKContext ctxt) {
        AssetManager assetManager = AssetManagerClient.getInstance();

        AssetBinary areaTexData = assetManager.loadBin("textures/areatex.bin");
        AssetBinary searchTexData = assetManager.loadBin("textures/searchtex.bin");
        areaTex = makeAATexture(ctxt, areaTexData, 160, 560, 320, VK_FORMAT_R8G8_UNORM);
        searchTex = makeAATexture(ctxt, searchTexData,  64, 16, 64, VK_FORMAT_R8_UNORM);
    }
    private VkTexture makeAATexture(VKContext ctxt, AssetBinary bin, int w, int h, int stride, int format) {
        VkTexture tex = new VkTexture(ctxt);
        TextureBinMips binMips = new TextureBinMips(bin.getData(), w, h);
        tex.build(format, binMips);
        tex.genView();
        return tex;
    }
    void destroy(VKContext ctxt) {
        edgeDetect.destroyPipeLine(ctxt);
        blendWeight.destroyPipeLine(ctxt);
        neighborBlend.destroyPipeLine(ctxt);
        this.fbedgeDetect.destroy();
        this.fbBlendWeight.destroy();
        this.fbOutput.destroy();
    }
    public VkSMAA(VKContext ctxt, final int quality) {
        this(ctxt, quality, false, false, false);
    }
    public VkSMAA(VKContext ctxt, final int quality, final boolean usePredTex, boolean srgb, final boolean reprojection) {
        if (!initTex) {
            initTex = true;
            initTextures(ctxt);
        }
        AssetManager assetManager = AssetManagerClient.getInstance();
        final int w = ctxt.swapChain.width;
        final int h = ctxt.swapChain.height;
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
        try ( MemoryStack stack = stackPush() ) 
        {
            
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_edgedetection.vsh", VK_SHADER_STAGE_VERTEX_BIT, def);
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_edgedetection.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, def);
            edgeDetect.setShaders(vert, frag);
            edgeDetect.setRenderPass(VkRenderPasses.passAAEdge, 0);
            edgeDetect.setScreenSpaceTriangle();
            edgeDetect.setBlend(false);
            edgeDetect.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            edgeDetect.depthStencilState.depthTestEnable(true);
            edgeDetect.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            edgeDetect.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            edgeDetect.viewport.width(w).height(h);
            edgeDetect.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
            edgeDetect.scissors.extent().width(w).height(h);
            edgeDetect.pipeline = edgeDetect.buildPipeline(ctxt);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_blend_weight.vsh", VK_SHADER_STAGE_VERTEX_BIT, def);
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_blend_weight.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, def);
            blendWeight.setShaders(vert, frag);
            blendWeight.setBlend(false);
            blendWeight.setRenderPass(VkRenderPasses.passAAWeight, 0);
            blendWeight.setScreenSpaceTriangle();
            blendWeight.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            blendWeight.depthStencilState.depthTestEnable(true);
            blendWeight.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            blendWeight.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            blendWeight.viewport.width(w).height(h);
            blendWeight.depthStencilState.depthCompareOp(VK_COMPARE_OP_EQUAL);
            blendWeight.scissors.extent().width(w).height(h);
            blendWeight.pipeline = blendWeight.buildPipeline(ctxt);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_neighbor_blend.vsh", VK_SHADER_STAGE_VERTEX_BIT, def);
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_neighbor_blend.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, def);
            neighborBlend.setShaders(vert, frag);
            neighborBlend.setBlend(true);
            neighborBlend.setRenderPass(VkRenderPasses.passFramebufferNoDepth, 0);
            neighborBlend.setScreenSpaceTriangle();
            neighborBlend.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            neighborBlend.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            neighborBlend.viewport.width(w).height(h);
            neighborBlend.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
            neighborBlend.scissors.extent().width(w).height(h);
            neighborBlend.pipeline = neighborBlend.buildPipeline(ctxt);
        }
        this.fbedgeDetect = new FrameBuffer(ctxt);
        this.fbedgeDetect.fromRenderpass(VkRenderPasses.passAAEdge, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbedgeDetect.build(VkRenderPasses.passAAEdge, w, h);
        this.fbBlendWeight = new FrameBuffer(ctxt);
        this.fbBlendWeight.fromRenderpass(VkRenderPasses.passAAWeight, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbBlendWeight.copyAtt(this.fbedgeDetect, 1);
        this.fbBlendWeight.build(VkRenderPasses.passAAWeight, w, h);
        this.fbOutput = new FrameBuffer(ctxt);
        this.fbOutput.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbOutput.build(VkRenderPasses.passFramebufferNoDepth, w, h);
        
        this.descBlendWeight = ctxt.descLayouts.allocDescSetSampleTriple();
        this.descNeighborBlend = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descOutput = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descOutputEdges = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descOutputWeights = ctxt.descLayouts.allocDescSetSampleSingle();
        
        FramebufferAttachment edgeColorAtt = this.fbedgeDetect.getAtt(0);
        
        this.descBlendWeight.setBindingCombinedImageSampler(0, edgeColorAtt.getView(), ctxt.samplerLinearClamp, edgeColorAtt.finalLayout);
        this.descBlendWeight.setBindingCombinedImageSampler(1, areaTex.getView(), ctxt.samplerLinearClamp, areaTex.getImageLayout());
        this.descBlendWeight.setBindingCombinedImageSampler(2, searchTex.getView(), ctxt.samplerLinearClamp, searchTex.getImageLayout());
        this.descBlendWeight.update(ctxt);

        FramebufferAttachment blendWeightColorAtt = this.fbBlendWeight.getAtt(0);
        this.descNeighborBlend.setBindingCombinedImageSampler(0, blendWeightColorAtt.getView(), ctxt.samplerLinearClamp, blendWeightColorAtt.finalLayout);
        this.descNeighborBlend.update(ctxt);

        FramebufferAttachment outputColorAtt = this.fbOutput.getAtt(0);
        this.descOutput.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutput.update(ctxt);
        outputColorAtt = this.fbedgeDetect.getAtt(0);
        this.descOutputEdges.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutputEdges.update(ctxt);
        outputColorAtt = this.fbBlendWeight.getAtt(0);
        this.descOutputWeights.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutputWeights.update(ctxt);

    }
    public void render(VkDescriptor descInput) {
        Engine.beginRenderPass(VkRenderPasses.passAAEdge, this.fbedgeDetect, VK_SUBPASS_CONTENTS_INLINE);
        Engine.clearAllDescriptorSets();
        Engine.setDescriptorSet(VkDescLayouts.DESC0, descInput);
        Engine.bindPipeline(this.edgeDetect);
        Engine.drawFSTri();
        Engine.endRenderPass();
        Engine.beginRenderPass(VkRenderPasses.passAAWeight, this.fbBlendWeight, VK_SUBPASS_CONTENTS_INLINE);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descBlendWeight);
        Engine.bindPipeline(this.blendWeight);
        Engine.drawFSTri();
        Engine.endRenderPass();
        Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, this.fbOutput, VK_SUBPASS_CONTENTS_INLINE);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descNeighborBlend);
        Engine.bindPipeline(this.neighborBlend);
        Engine.drawFSTri();
        Engine.endRenderPass();
    }
    
}
