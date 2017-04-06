/**
 * 
 */
package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.vulkan.VkPipelines.VkShaderDef;

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
    public static VkPipelineLayout pipelineLayoutTemporalResolve = new VkPipelineLayout("pipelineLayoutTemporalResolve");
    private boolean initTex = false;
    private static VkTexture areaTex;
    private static VkTexture searchTex;
    public VkPipelineGraphics edgeDetect = new VkPipelineGraphics(pipelineLayoutEdgeDetect);
    public VkPipelineGraphics blendWeight = new VkPipelineGraphics(pipelineLayoutBlendWeight);
    public VkPipelineGraphics neighborBlend = new VkPipelineGraphics(pipelineLayoutNeighborBlend);
    public VkPipelineGraphics temporalResolve = new VkPipelineGraphics(pipelineLayoutTemporalResolve);
    private VkDescriptor descBlendWeight;
    private VkDescriptor descNeighborBlend;
    private FrameBuffer fbedgeDetect;
    private FrameBuffer fbBlendWeight;
    private FrameBuffer fbOutput0;
    private FrameBuffer fbOutput1;
    private FrameBuffer fbResolved;
    public VkDescriptor descOutput0;
    public VkDescriptor descOutput1;
    public VkDescriptor descOutputEdges;
    public VkDescriptor descOutputWeights;
    public final boolean usePredTex;
    public final boolean useReprojection;
    private int frame;
    private VkDescriptor descOutputResolved;

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
        this.fbOutput0.destroy();
        if (this.fbOutput1 != null)
            this.fbOutput1.destroy();   
        if (this.fbResolved != null)
            this.fbResolved.destroy();   
    }
    public VkSMAA(VKContext ctxt, final int quality) {
        this(ctxt, quality, false, false, false);
    }
    public VkSMAA(VKContext ctxt, final int quality, final boolean usePredTex, boolean srgb, final boolean reprojection) {
        if (!initTex) {
            initTex = true;
            initTextures(ctxt);
        }
        this.usePredTex = usePredTex;
        this.useReprojection = reprojection;
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
            neighborBlend.setBlend(false);
            neighborBlend.setRenderPass(VkRenderPasses.passFramebufferNoDepth, 0);
            neighborBlend.setScreenSpaceTriangle();
            neighborBlend.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            neighborBlend.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            neighborBlend.viewport.width(w).height(h);
            neighborBlend.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
            neighborBlend.scissors.extent().width(w).height(h);
            neighborBlend.pipeline = neighborBlend.buildPipeline(ctxt);
        }
        if (this.useReprojection) {
            try ( MemoryStack stack = stackPush() ) 
            {
                VkShader vert = ctxt.loadCompileGLSL(assetManager, "screen_triangle.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
                VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/SMAA/SMAA_temporal_resolve.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, def);
                temporalResolve.setShaders(vert, frag);
                temporalResolve.setBlend(false);
                temporalResolve.setRenderPass(VkRenderPasses.passFramebufferNoDepth, 0);
                temporalResolve.setScreenSpaceTriangle();
                temporalResolve.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
                temporalResolve.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
                temporalResolve.viewport.width(w).height(h);
                temporalResolve.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
                temporalResolve.scissors.extent().width(w).height(h);
                temporalResolve.pipeline = temporalResolve.buildPipeline(ctxt);
            }
        }
        this.fbedgeDetect = new FrameBuffer(ctxt);
        this.fbedgeDetect.fromRenderpass(VkRenderPasses.passAAEdge, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbedgeDetect.build(VkRenderPasses.passAAEdge, w, h);
        this.fbBlendWeight = new FrameBuffer(ctxt);
        this.fbBlendWeight.fromRenderpass(VkRenderPasses.passAAWeight, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbBlendWeight.copyAtt(this.fbedgeDetect, 1);
        this.fbBlendWeight.build(VkRenderPasses.passAAWeight, w, h);
        this.fbOutput0 = new FrameBuffer(ctxt);
        this.fbOutput0.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbOutput0.build(VkRenderPasses.passFramebufferNoDepth, w, h);
        if (this.useReprojection) {
            this.fbOutput1 = new FrameBuffer(ctxt);
            this.fbOutput1.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
            this.fbOutput1.build(VkRenderPasses.passFramebufferNoDepth, w, h);
            this.fbResolved = new FrameBuffer(ctxt);
            this.fbResolved.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
            this.fbResolved.build(VkRenderPasses.passFramebufferNoDepth, w, h);
        }
        
        this.descBlendWeight = ctxt.descLayouts.allocDescSetSampleTriple();
        this.descNeighborBlend = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descOutput0 = ctxt.descLayouts.allocDescSetSampleSingle();
        if (this.useReprojection) {
            this.descOutput1 = ctxt.descLayouts.allocDescSetSampleSingle();
            this.descOutputResolved = ctxt.descLayouts.allocDescSetSampleSingle();
        }
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

        FramebufferAttachment outputColorAtt = this.fbOutput0.getAtt(0);
        this.descOutput0.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutput0.update(ctxt);
        if (this.useReprojection) {
            outputColorAtt = this.fbOutput1.getAtt(0);
            this.descOutput1.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
            this.descOutput1.update(ctxt);
            outputColorAtt = this.fbResolved.getAtt(0);
            this.descOutputResolved.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
            this.descOutputResolved.update(ctxt);
        }
        outputColorAtt = this.fbedgeDetect.getAtt(0);
        this.descOutputEdges.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutputEdges.update(ctxt);
        outputColorAtt = this.fbBlendWeight.getAtt(0);
        this.descOutputWeights.setBindingCombinedImageSampler(0, outputColorAtt.getView(), ctxt.samplerLinearClamp, outputColorAtt.finalLayout);
        this.descOutputWeights.update(ctxt);

    }
    public VkDescriptor render(VkDescriptor descInput, VkDescriptor descTexMaterial, VkDescriptor descTextureVelocityBuffer) {
        this.frame = 1 - this.frame;
        Engine.beginRenderPass(VkRenderPasses.passAAEdge, this.fbedgeDetect);
        Engine.clearAllDescriptorSets();
        Engine.setDescriptorSet(VkDescLayouts.DESC0, descInput);
        if (this.usePredTex)
            Engine.setDescriptorSet(VkDescLayouts.DESC1, descTexMaterial);
        Engine.bindPipeline(this.edgeDetect);
        Engine.drawFSTri();
        Engine.endRenderPass();
        Engine.beginRenderPass(VkRenderPasses.passAAWeight, this.fbBlendWeight);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descBlendWeight);
        Engine.bindPipeline(this.blendWeight);
        if (this.useReprojection) {
            PushConstantBuffer buf = PushConstantBuffer.INST;
            int n = Engine.getTemporalJitterIdx();
            if (n%2==0)
                buf.setVec4(0, 1, 1, 1, 0);
            else 
                buf.setVec4(0, 2, 2, 2, 0);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), this.blendWeight.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(4*4));
        }
        Engine.drawFSTri();
        Engine.endRenderPass();
        FrameBuffer buffer = this.frame == 0 || !this.useReprojection ? this.fbOutput0 : this.fbOutput1;
        Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, buffer);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descNeighborBlend);
        if (this.useReprojection)
            Engine.setDescriptorSet(VkDescLayouts.DESC2, descTextureVelocityBuffer);
        Engine.bindPipeline(this.neighborBlend);
        Engine.drawFSTri();
        Engine.endRenderPass();
        if (!this.useReprojection)
        {
            return this.descOutput0;
        }
        Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, this.fbResolved);
        Engine.setDescriptorSet(VkDescLayouts.DESC0, this.frame == 0 ? this.descOutput0 : this.descOutput1);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.frame == 0 ? this.descOutput1 : this.descOutput0);
        Engine.setDescriptorSet(VkDescLayouts.DESC2, descTextureVelocityBuffer);
        Engine.bindPipeline(this.temporalResolve);
        Engine.drawFSTri();
        Engine.endRenderPass();
        return this.descOutputResolved;
    }
    
}
