package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkViewport;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLVAO;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.UniformBuffer;

public class VkPipelines {
    public static VkPipelineLayout pipelineLayoutTextured = new VkPipelineLayout();
    public static VkPipelineLayout pipelineLayoutGUI = new VkPipelineLayout();
    public static VkPipeline main = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline screen2d = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline fontRender2D = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline colored2D = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline gui = new VkPipeline(VkPipelines.pipelineLayoutGUI);
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
        {
            main.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[1|2].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            main.setShaders(vert, frag);
            main.setBlend(false);
            main.setRenderPass(ctxt.getMainRenderPass(), 0);
            main.setVertexDesc(desc);
            main.pipeline = buildPipeLine(ctxt, main);
        }

        {
            screen2d.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            screen2d.setShaders(vert, frag);
            screen2d.setBlend(false);
            screen2d.setRenderPass(ctxt.getMainRenderPass(), 1);
            screen2d.setVertexDesc(desc);
            screen2d.pipeline = buildPipeLine(ctxt, screen2d);
        }

        {
            fontRender2D.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            fontRender2D.setShaders(vert, frag);
            fontRender2D.setBlend(true);
            fontRender2D.setRenderPass(ctxt.getMainRenderPass(), 1);
            fontRender2D.setVertexDesc(desc);
            fontRender2D.pipeline = buildPipeLine(ctxt, fontRender2D);
        }

        {
            colored2D.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "colored.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "colored.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            colored2D.setShaders(vert, frag);
            colored2D.setBlend(true);
            colored2D.setRenderPass(ctxt.getMainRenderPass(), 1);
            colored2D.setVertexDesc(desc);
            colored2D.pipeline = buildPipeLine(ctxt, colored2D);
        }

        {
            gui.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "gui.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "gui.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            gui.setShaders(vert, frag);
            gui.setBlend(true);
            gui.setRenderPass(ctxt.getMainRenderPass(), 1);
            gui.setVertexDesc(desc);
            gui.pipeline = buildPipeLine(ctxt, gui);
        }
    }

    static long buildPipeLine(VKContext vkContext, VkPipeline pipe) {
        pipe.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
        pipe.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
        pipe.viewport.width(vkContext.swapChain.width).height(vkContext.swapChain.height);
        pipe.scissors.extent().width(vkContext.swapChain.width).height(vkContext.swapChain.height);
        pipe.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
        long pipeline = pipe.buildPipeline(vkContext);
        return pipeline;
    }

    public static void destroyShutdown(VKContext vkContext) {
        main.destroy(vkContext);
        screen2d.destroy(vkContext);
        fontRender2D.destroy(vkContext);
        colored2D.destroy(vkContext);
        gui.destroy(vkContext);
        pipelineLayoutTextured.destroy(vkContext);
        pipelineLayoutGUI.destroy(vkContext);
    }


}
