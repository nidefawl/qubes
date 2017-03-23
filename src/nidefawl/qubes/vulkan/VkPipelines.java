package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLVAO;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.shader.IShaderDef;

public class VkPipelines {
    public static VkPipeline[] arrPipe = new VkPipeline[24];
    public static VkPipelineLayout[] arrLayout = new VkPipelineLayout[24];
    
    public static VkPipelineLayout pipelineLayoutTextured = new VkPipelineLayout("pipelineLayoutTextured");
    public static VkPipelineLayout pipelineLayoutMain = new VkPipelineLayout("pipelineLayoutMain");
    public static VkPipelineLayout pipelineLayoutTerrain = new VkPipelineLayout("pipelineLayoutTerrain");
    public static VkPipelineLayout pipelineLayoutColored = new VkPipelineLayout("pipelineLayoutColored");
    public static VkPipelineLayout pipelineLayoutShadow = new VkPipelineLayout("pipelineLayoutShadow");
    public static VkPipelineLayout pipelineLayoutGUI = new VkPipelineLayout("pipelineLayoutGUI");
    public static VkPipelineLayout pipelineLayoutSingleBlock = new VkPipelineLayout("pipelineLayoutSingleBlock");
    public static VkPipelineLayout pipelineLayoutDeferredPass0 = new VkPipelineLayout("pipelineLayoutDeferredPass0");
    public static VkPipelineLayout pipelineLayoutDeferredPass1 = new VkPipelineLayout("pipelineLayoutDeferredPass1");
    public static VkPipelineLayout pipelineLayoutTonemapDynamic = new VkPipelineLayout("pipelineLayoutTonemapDynamic");
    public static VkPipelineLayout pipelineLayoutSkybox = new VkPipelineLayout("pipelineLayoutSkyboxBackground");
    public static VkPipelineLayout pipelineLayoutSkyboxSprites = new VkPipelineLayout("pipelineLayoutSkyboxSprites");
    
    public static VkPipeline shadowSolid = new VkPipeline(VkPipelines.pipelineLayoutShadow);
    public static VkPipeline shadowDebug = new VkPipeline(VkPipelines.pipelineLayoutShadow);
    public static VkPipeline main = new VkPipeline(VkPipelines.pipelineLayoutMain);
    public static VkPipeline textured2d = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline debugShader = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline fontRender2D = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline colored2D = new VkPipeline(VkPipelines.pipelineLayoutColored);
    public static VkPipeline gui = new VkPipeline(VkPipelines.pipelineLayoutGUI);
    public static VkPipeline terrain = new VkPipeline(VkPipelines.pipelineLayoutTerrain);
    public static VkPipeline water = new VkPipeline(VkPipelines.pipelineLayoutTerrain);
    public static VkPipeline item = new VkPipeline(VkPipelines.pipelineLayoutTextured);
    public static VkPipeline singleblock = new VkPipeline(VkPipelines.pipelineLayoutSingleBlock);
    public static VkPipeline deferred_pass0 = new VkPipeline(VkPipelines.pipelineLayoutDeferredPass0);
    public static VkPipeline deferred_pass1 = new VkPipeline(VkPipelines.pipelineLayoutDeferredPass1);
    public static VkPipeline tonemapDynamic = new VkPipeline(VkPipelines.pipelineLayoutTonemapDynamic);
    public static VkPipeline skybox_update_background = new VkPipeline(VkPipelines.pipelineLayoutSkybox);
    public static VkPipeline skybox_update_background_cubemap = new VkPipeline(VkPipelines.pipelineLayoutSkybox);
    public static VkPipeline skybox_update_sprites = new VkPipeline(VkPipelines.pipelineLayoutSkyboxSprites);
    public static VkPipeline skybox_sample = new VkPipeline(VkPipelines.pipelineLayoutSkybox);
    public static VkPipeline skybox_sample_single = new VkPipeline(VkPipelines.pipelineLayoutSkybox);

    
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
        public VkShaderDef() {
            this(null, null);
        }
        @Override
        public String getDefinition(String define) {
            if ("VK_VERTEX_ATTRIBUTES".equals(define)) {
                if (this.desc == null)
                    return null;
                return this.desc.getVertexDefGLSL();
            }
            return extended != null? extended.getDefinition(define) : null;
        }
    }

    static final class DeferredDefs implements IShaderDef {
        private final int pass;
        public DeferredDefs(int pass) {
            this.pass = pass;
        }
        @Override
        public String getDefinition(String define) {
            if ("RENDER_PASS".equals(define)) {
                return "#define RENDER_PASS "+pass;
            }
            if ("RENDER_AMBIENT_OCCLUSION".equals(define)) {
//                return "#define RENDER_AMBIENT_OCCLUSION "+(Engine.RENDER_SETTINGS.ao%2);
                return null;
            }
            if ("RENDER_VELOCITY_BUFFER".equals(define)) {
//                return "#define RENDER_VELOCITY_BUFFER "+(Engine.getRenderVelocityBuffer() ? "1" : "0");
                return null;
            }
            if ("RENDER_MATERIAL_BUFFER".equals(define)) {
//                return "#define RENDER_MATERIAL_BUFFER "+(Engine.getRenderMaterialBuffer() ? "1" : "0"); 
            }
            if ("BLUE_NOISE".equals(define)) {
//                return "#define BLUE_NOISE"; 
            }
            return null;
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
            shadowSolid.depthStencilState.depthCompareOp(Engine.INVERSE_MAP ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);

            shadowSolid.scissors.extent().width(Engine.getShadowMapTextureSize()).height(Engine.getShadowMapTextureSize());
            shadowSolid.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_DEPTH_BIAS));
            shadowSolid.rasterizationState.depthBiasEnable(true);
            shadowSolid.rasterizationState.cullMode(VK_CULL_MODE_BACK_BIT);
            shadowSolid.pipeline = buildPipeLine(ctxt, shadowSolid);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            shadowDebug.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocksShadow.getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "shadow/shadow_debug.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "shadow/shadow_debug.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            shadowDebug.setShaders(vert, frag);
            shadowDebug.setBlend(false);
            shadowDebug.setRenderPass(VkRenderPasses.passShadow, 0);
            shadowDebug.setVertexDesc(desc);
            shadowDebug.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            shadowDebug.useSwapChainViewport = false;
            shadowDebug.viewport.minDepth(Engine.INVERSE_MAP?1:0);
            shadowDebug.viewport.maxDepth(Engine.INVERSE_MAP?1:0);
            shadowDebug.depthStencilState.depthCompareOp(Engine.INVERSE_MAP ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);

            shadowDebug.viewport.width(Engine.getShadowMapTextureSize()).height(Engine.getShadowMapTextureSize());
            shadowDebug.rasterizationState.frontFace(!Engine.INVERSE_MAP?VK_FRONT_FACE_CLOCKWISE:VK_FRONT_FACE_COUNTER_CLOCKWISE);
            shadowDebug.scissors.extent().width(Engine.getShadowMapTextureSize()).height(Engine.getShadowMapTextureSize());
            shadowDebug.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT));
//            shadowDebug.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_DEPTH_BIAS));
//            shadowDebug.rasterizationState.depthBiasEnable(true);
//            shadowDebug.rasterizationState.cullMode(VK_CULL_MODE_BACK_BIT);
//            shadowDebug.depthStencilState.depthWriteEnable(true);
//            shadowDebug.depthStencilState.depthTestEnable(true);
            shadowDebug.pipeline = buildPipeLine(ctxt, shadowDebug);
        }
        {
            terrain.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocks.getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "terrain/terrain.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "terrain/terrain.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            terrain.setShaders(vert, frag);
            terrain.setBlend(false);
            terrain.setRenderPass(VkRenderPasses.passTerrain_Pass0, 0);
            terrain.setVertexDesc(desc);
            terrain.pipeline = buildPipeLine(ctxt, terrain);
        }
        {
            water.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocks.getVkVertexDesc();
            VKContext.DUMP_SHADER_SRC=true;
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "terrain/water.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "terrain/water.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);

            VKContext.DUMP_SHADER_SRC=false;
            water.setShaders(vert, frag);
            water.setBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO);
            water.setRenderPass(VkRenderPasses.passTerrain_Pass1, 0);
            water.setVertexDesc(desc);
            water.pipeline = buildPipeLine(ctxt, water);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            main.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[1|2].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk_shaded.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured_3Dvk_shaded.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("MAP_Z_INVERSE".equals(define)) {
                        return Engine.INVERSE_MAP?"#define MAP_Z_INVERSE 1":"#define MAP_Z_INVERSE 0";
                    }
                    return null;
                }
            });
            main.useSwapChainViewport = true;
            main.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            main.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT));

            main.setShaders(vert, frag);
            main.setBlend(false);
            main.setRenderPass(VkRenderPasses.passFramebuffer, 0);
            main.setVertexDesc(desc);
            main.pipeline = buildPipeLine(ctxt, main);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            textured2d.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "textured.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            textured2d.setShaders(vert, frag);
            textured2d.setBlend(true);
            textured2d.setRenderPass(VkRenderPasses.passFramebuffer, 0);
            textured2d.setVertexDesc(desc);
            textured2d.dynamicState=null;
            textured2d.pipeline = buildPipeLine(ctxt, textured2d);
            textured2d.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            textured2d.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));

            textured2d.pipelineScissors = buildPipeLine(ctxt, textured2d);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            skybox_sample_single.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "sky/skybox_sample_2d.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "sky/skybox_sample_2d.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            skybox_sample_single.depthStencilState.depthTestEnable(false);
            skybox_sample_single.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            skybox_sample_single.setShaders(vert, frag);
            skybox_sample_single.setBlend(false);
            skybox_sample_single.setRenderPass(VkRenderPasses.passTerrain_Pass0, 0);
            skybox_sample_single.setEmptyVertexInput();
            skybox_sample_single.dynamicState=null;
            skybox_sample_single.pipeline = buildPipeLine(ctxt, skybox_sample_single);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            skybox_update_background.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "sky/skybox_generate.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "sky/skybox_generate.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            skybox_update_background.depthStencilState.depthTestEnable(false);
            skybox_update_background.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            skybox_update_background.setShaders(vert, frag);
            skybox_update_background.setBlend(false);
            skybox_update_background.setRenderPass(VkRenderPasses.passSkyGenerate, 0);
            skybox_update_background.setEmptyVertexInput();
            skybox_update_background.dynamicState=null;
            skybox_update_background.pipeline = buildPipeLine(ctxt, skybox_update_background);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            skybox_update_sprites.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "particle/clouds.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "particle/clouds.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            skybox_update_sprites.useSwapChainViewport = false;
            skybox_update_sprites.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            skybox_update_sprites.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            skybox_update_sprites.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
            skybox_update_sprites.scissors.extent().width(SkyRenderer.SKYBOX_RES).height(SkyRenderer.SKYBOX_RES);

            skybox_update_sprites.depthStencilState.depthTestEnable(false);
            skybox_update_sprites.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            skybox_update_sprites.setShaders(vert, frag);
            skybox_update_sprites.setBlend(false);
            skybox_update_sprites.setRenderPass(VkRenderPasses.passSkyGenerateCubemap, 0);
            skybox_update_sprites.derivedPipeDef = new IDerivedPipeDef() {

                @Override
                public int getNumDerived() {
                    return 5;
                }

                @Override
                public void setPipeDef(int i, VkGraphicsPipelineCreateInfo subPipeline) {
                    subPipeline.subpass(i);
                }
                
            };

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.callocStack(2);
            attributeDescriptions.get(0).binding(0).location(0).format(VK_FORMAT_R16G16_SFLOAT).offset(0);
            attributeDescriptions.get(1).binding(1).location(1).format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(0);
            attributeDescriptions.get(1).binding(1).location(2).format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(16);
            skybox_update_sprites.bindingDescriptions = VkVertexInputBindingDescription.callocStack(2);
            skybox_update_sprites.bindingDescriptions.get(0)
                .binding(0)
                .stride(4)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            skybox_update_sprites.bindingDescriptions.get(1)
                .binding(1)
                .stride(32)
                .inputRate(VK_VERTEX_INPUT_RATE_INSTANCE);
            skybox_update_sprites.vertexInputState.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(skybox_update_sprites.bindingDescriptions)
                .pVertexAttributeDescriptions(attributeDescriptions);
            skybox_update_sprites.dynamicState=null;
            skybox_update_sprites.pipeline = buildPipeLine(ctxt, skybox_update_sprites);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            skybox_update_background_cubemap.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "sky/skybox_generate.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "sky/skybox_generate.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            skybox_update_background_cubemap.useSwapChainViewport = false;
            skybox_update_background_cubemap.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            skybox_update_background_cubemap.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            skybox_update_background_cubemap.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
            skybox_update_background_cubemap.scissors.extent().width(SkyRenderer.SKYBOX_RES).height(SkyRenderer.SKYBOX_RES);

            skybox_update_background_cubemap.depthStencilState.depthTestEnable(false);
            skybox_update_background_cubemap.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            skybox_update_background_cubemap.setShaders(vert, frag);
            skybox_update_background_cubemap.setBlend(false);
            skybox_update_background_cubemap.setRenderPass(VkRenderPasses.passSkyGenerateCubemap, 0);
            skybox_update_background_cubemap.derivedPipeDef = new IDerivedPipeDef() {

                @Override
                public int getNumDerived() {
                    return 5;
                }

                @Override
                public void setPipeDef(int i, VkGraphicsPipelineCreateInfo subPipeline) {
                    subPipeline.subpass(i);
                }
                
            };
            skybox_update_background_cubemap.setEmptyVertexInput();
            skybox_update_background_cubemap.dynamicState=null;
            skybox_update_background_cubemap.pipeline = buildPipeLine(ctxt, skybox_update_background_cubemap);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            skybox_sample.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "sky/skybox_sample_cubemap.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());

            VkShader frag = ctxt.loadCompileGLSL(assetManager, "sky/skybox_sample_cubemap.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            skybox_sample.depthStencilState.depthTestEnable(false);
            skybox_sample.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            skybox_sample.setShaders(vert, frag);
            skybox_sample.setBlend(false);
            skybox_sample.setRenderPass(VkRenderPasses.passTerrain_Pass0, 0);
            skybox_sample.setEmptyVertexInput();
            skybox_sample.dynamicState=null;
            skybox_sample.pipeline = buildPipeLine(ctxt, skybox_sample);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            deferred_pass0.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "post/deferred.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());

            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/deferred.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, new DeferredDefs(0));
            deferred_pass0.depthStencilState.depthTestEnable(false);
            deferred_pass0.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            deferred_pass0.setShaders(vert, frag);
            deferred_pass0.setBlend(false);
            deferred_pass0.setRenderPass(VkRenderPasses.passDeferred, 0);
            deferred_pass0.setEmptyVertexInput();
            deferred_pass0.pipeline = buildPipeLine(ctxt, deferred_pass0);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            deferred_pass1.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "post/deferred.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/deferred.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, new DeferredDefs(1));
            deferred_pass1.depthStencilState.depthTestEnable(false);
            deferred_pass1.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            deferred_pass1.setShaders(vert, frag);
            deferred_pass1.setBlend(true);
            deferred_pass1.setRenderPass(VkRenderPasses.passDeferred, 0);
            deferred_pass1.setEmptyVertexInput();
            deferred_pass1.pipeline = buildPipeLine(ctxt, deferred_pass1);
        }
        try ( MemoryStack stack = stackPush() ) 
        {
            tonemapDynamic.destroyPipeLine(ctxt);
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "screen_triangle.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef());
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "post/finalstage.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("DO_AUTOEXPOSURE".equals(define))
                        return "#define DO_AUTOEXPOSURE";
                    return null;
                }
            });
            tonemapDynamic.depthStencilState.depthTestEnable(false);
            tonemapDynamic.rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);
            tonemapDynamic.setShaders(vert, frag);
            tonemapDynamic.setBlend(false);
            tonemapDynamic.setRenderPass(VkRenderPasses.passFramebufferNoDepth, 0);
            tonemapDynamic.setEmptyVertexInput();
            tonemapDynamic.pipeline = buildPipeLine(ctxt, tonemapDynamic);
        }

        try ( MemoryStack stack = stackPush() ) 
        {
            item.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4|8].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "item.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "item.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            item.setShaders(vert, frag);
            item.setBlend(true);
            item.setRenderPass(VkRenderPasses.passFramebuffer, 0);
            item.setVertexDesc(desc);
            item.dynamicState=null;
            item.pipeline = buildPipeLine(ctxt, item);
            item.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            item.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
            item.pipelineScissors = buildPipeLine(ctxt, item);
        }
        
        try ( MemoryStack stack = stackPush() ) 
        {
            singleblock.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoBlocks.getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "singleblock.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "singleblock.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            singleblock.useSwapChainViewport = false;
            singleblock.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            singleblock.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            singleblock.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);

            singleblock.scissors.extent().width(SingleBlockRenderAtlas.getTexSize()).height(SingleBlockRenderAtlas.getTexSize());
            singleblock.setShaders(vert, frag);
            singleblock.setBlend(false);
            singleblock.setRenderPass(VkRenderPasses.passFramebuffer, 0);
            singleblock.setVertexDesc(desc);
            singleblock.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            singleblock.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT));
            singleblock.pipeline = buildPipeLine(ctxt, singleblock);
            singleblock.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_VIEWPORT));
            singleblock.pipelineScissors = buildPipeLine(ctxt, singleblock);

        }

        {
            debugShader.destroyPipeLine(ctxt);
            VkVertexDescriptors desc = GLVAO.vaoTesselator[2|4].getVkVertexDesc();
            VkShader vert = ctxt.loadCompileGLSL(assetManager, "shadow/debug.vsh", VK_SHADER_STAGE_VERTEX_BIT, new VkShaderDef(desc));
            VkShader frag = ctxt.loadCompileGLSL(assetManager, "shadow/debug.fsh", VK_SHADER_STAGE_FRAGMENT_BIT, null);
            debugShader.depthStencilState.depthTestEnable(false);
            debugShader.rasterizationState.cullMode(VK_CULL_MODE_NONE);
            debugShader.setShaders(vert, frag);
            debugShader.setBlend(false);
            debugShader.setRenderPass(VkRenderPasses.passFramebuffer, 0);
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
            fontRender2D.setRenderPass(VkRenderPasses.passFramebuffer, 0);
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
            colored2D.setRenderPass(VkRenderPasses.passFramebuffer, 0);
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
            gui.setRenderPass(VkRenderPasses.passFramebuffer, 0);
            gui.setVertexDesc(desc);
            gui.dynamicState = null;
            gui.pipeline = buildPipeLine(ctxt, gui);
            gui.dynamicState = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            gui.dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
            gui.pipelineScissors = buildPipeLine(ctxt, gui);
        }
    }

    static long buildPipeLine(VKContext vkContext, VkPipeline pipe) {
        if (pipe.useSwapChainViewport) {
            pipe.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
            pipe.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            pipe.viewport.width(vkContext.swapChain.width).height(vkContext.swapChain.height);
            pipe.scissors.extent().width(vkContext.swapChain.width).height(vkContext.swapChain.height);
            pipe.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);

        }
        long pipeline = pipe.buildPipeline(vkContext);
        return pipeline;
    }

    public static void destroyShutdown(VKContext ctxt) {
        for (int i = 0; i < arrPipe.length; i++) {
            if (arrPipe[i] != null) {
                arrPipe[i].destroy(ctxt);
                arrPipe[i] = null;
            }
        }
        for (int i = 0; i < arrLayout.length; i++) {
            if (arrLayout[i] != null) {
                arrLayout[i].destroy(ctxt);
                arrLayout[i] = null;
            }
        }
    }

    public static void registerLayout(VkPipelineLayout vkPipelineLayout) {
        for (int i = 0; i < arrLayout.length; i++) {
            if (arrLayout[i] == null) {
                arrLayout[i] = vkPipelineLayout;
                break;
            }
        }
    }

    public static void registerPipe(VkPipeline vkPipeline) {
        for (int i = 0; i < arrPipe.length; i++) {
            if (arrPipe[i] == null) {
                arrPipe[i] = vkPipeline;
                break;
            }
        }
    }


}
