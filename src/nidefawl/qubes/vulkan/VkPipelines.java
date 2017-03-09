package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLVAO;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.UniformBuffer;

public class VkPipelines {
    public static VkPipelineLayout pipelineLayoutTextured = new VkPipelineLayout("pipelineLayoutTextured");
    public static VkPipelineLayout pipelineLayoutMain = new VkPipelineLayout("pipelineLayoutMain");
    public static VkPipelineLayout pipelineLayoutTerrain = new VkPipelineLayout("pipelineLayoutTerrain");
    public static VkPipelineLayout pipelineLayoutColored = new VkPipelineLayout("pipelineLayoutColored");
    public static VkPipelineLayout pipelineLayoutShadow = new VkPipelineLayout("pipelineLayoutShadow");
    public static VkPipelineLayout pipelineLayoutGUI = new VkPipelineLayout("pipelineLayoutGUI");
    public static VkPipeline shadowSolid = new VkPipeline(VkPipelines.pipelineLayoutShadow);
    public static VkPipeline main = new VkPipeline(VkPipelines.pipelineLayoutMain);
    public static VkPipeline textured2d = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline debugShader = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline fontRender2D = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline colored2D = new VkPipeline(VkPipelines.pipelineLayoutColored);
    public static VkPipeline gui = new VkPipeline(VkPipelines.pipelineLayoutGUI);
    public static VkPipeline terrain = new VkPipeline(VkPipelines.pipelineLayoutTerrain);
    static {
    }
    static class VkShaderDef implements IShaderDef {
        private final VkVertexDescriptors desc;
        private final IShaderDef extended;
        public VkShaderDef(VkVertexDescriptors desc) {
            this(desc, null);
        }
        public VkShaderDef(VkVertexDescriptors desc, IShaderDef extended) {
            this.desc = desc;
            this.extended = extended;
        }
        @Override
        public String getDefinition(String define) {
            if ("VK_VERTEX_ATTRIBUTES".equals(define)) {
                return this.desc.getVertexDefGLSL();
            }
            return extended != null? extended.getDefinition(define) : null;
        }
    }
    public static void init(VKContext ctxt) {
        AssetManager assetManager = AssetManagerClient.getInstance();
        try ( MemoryStack stack = stackPush() ) 
        {
            shadowSolid.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocksShadow.getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "shadow/shadow_solid.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "shadow/shadow_solid.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            shadowSolid.setShaders(vert, frag);
            shadowSolid.setBlend(false);
            shadowSolid.setRenderPass(VkRenderPasses.passShadow, 0);
            shadowSolid.setVertexDesc(desc);
            shadowSolid.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            shadowSolid.useSwapChainViewport = false;
            shadowSolid.viewport.width(Engine.getShadowMapTextureSize()).height(Engine.getShadowMapTextureSize());
            shadowSolid.scissors.extent().width(Engine.getShadowMapTextureSize()).height(Engine.getShadowMapTextureSize());
//            shadowSolid.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT));
            shadowSolid.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_DEPTH_BIAS));
            shadowSolid.rasterizationState.depthBiasEnable(true);
            shadowSolid.rasterizationState.cullMode(VK_CULL_MODE_FRONT_BIT);
            shadowSolid.pipeline = buildPipeLine(ctxt, shadowSolid);
        }
        {
            terrain.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocks.getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "terrain/terrain.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "terrain/terrain.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            terrain.setShaders(vert, frag);
            terrain.setBlend(false);
            terrain.setRenderPass(VkRenderPasses.passTerrain, 0);
            terrain.setVertexDesc(desc);
            terrain.pipeline = buildPipeLine(ctxt, terrain);
        }
        {
            main.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[1|2].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk_shaded.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk_shaded.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            main.setShaders(vert, frag);
            main.setBlend(false);
            main.setRenderPass(VkRenderPasses.passSubpassSwapchain, 0);
            main.setVertexDesc(desc);
            main.pipeline = buildPipeLine(ctxt, main);
        }

        {
            textured2d.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            textured2d.setShaders(vert, frag);
            textured2d.setBlend(false);
            textured2d.setRenderPass(VkRenderPasses.passSubpassSwapchain, 1);
            textured2d.setVertexDesc(desc);
            textured2d.pipeline = buildPipeLine(ctxt, textured2d);
        }

        {
            debugShader.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "shadow/debug.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "shadow/debug.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            debugShader.setShaders(vert, frag);
            debugShader.setBlend(false);
            debugShader.setRenderPass(VkRenderPasses.passSubpassSwapchain, 1);
            debugShader.setVertexDesc(desc);
            debugShader.pipeline = buildPipeLine(ctxt, debugShader);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            fontRender2D.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            fontRender2D.setShaders(vert, frag);
            fontRender2D.setBlend(true);
            fontRender2D.setRenderPass(VkRenderPasses.passSubpassSwapchain, 1);
            fontRender2D.setVertexDesc(desc);
            fontRender2D.dynamicState = null;
            fontRender2D.pipeline = buildPipeLine(ctxt, fontRender2D);
            fontRender2D.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            fontRender2D.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
            fontRender2D.pipelineScissors = buildPipeLine(ctxt, fontRender2D);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            colored2D.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "colored.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "colored.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            colored2D.setShaders(vert, frag);
            colored2D.setBlend(true);
            colored2D.setRenderPass(VkRenderPasses.passSubpassSwapchain, 1);
            colored2D.setVertexDesc(desc);
            colored2D.dynamicState = null;
            colored2D.pipeline = buildPipeLine(ctxt, colored2D);
            colored2D.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            colored2D.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
            colored2D.pipelineScissors = buildPipeLine(ctxt, colored2D);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            gui.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "gui.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "gui.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            gui.setShaders(vert, frag);
            gui.setBlend(true);
            gui.setRenderPass(VkRenderPasses.passSubpassSwapchain, 1);
            gui.setVertexDesc(desc);
            gui.dynamicState = null;
            gui.pipeline = buildPipeLine(ctxt, gui);
            gui.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            gui.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
            gui.pipelineScissors = buildPipeLine(ctxt, gui);
        }
    }

    static long buildPipeLine(VKContext vkContext, VkPipeline pipe) {
        pipe.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
        pipe.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
        if (pipe.useSwapChainViewport) {
            pipe.viewport.width(vkContext.swapChain.width).height(vkContext.swapChain.height);
            pipe.scissors.extent().width(vkContext.swapChain.width).height(vkContext.swapChain.height);
        }
        pipe.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
        long pipeline = pipe.buildPipeline(vkContext);
        return pipeline;
    }

    public static void destroyShutdown(VKContext vkContext) {
        main.destroy(vkContext);
        textured2d.destroy(vkContext);
        fontRender2D.destroy(vkContext);
        colored2D.destroy(vkContext);
        gui.destroy(vkContext);
        terrain.destroy(vkContext);
        debugShader.destroy(vkContext);
        shadowSolid.destroy(vkContext);
        pipelineLayoutTextured.destroy(vkContext);
        pipelineLayoutColored.destroy(vkContext);
        pipelineLayoutGUI.destroy(vkContext);
        pipelineLayoutShadow.destroy(vkContext);
    }


}
