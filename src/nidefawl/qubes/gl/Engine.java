package nidefawl.qubes.gl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Map;

import org.lwjgl.opengl.*;
import org.lwjgl.vulkan.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.config.RenderSettings;
import nidefawl.qubes.font.TrueTypeFont;
import nidefawl.qubes.gl.GLVAO.VertexAttrib;
import nidefawl.qubes.item.ItemRenderer;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.render.*;
import nidefawl.qubes.render.gui.SingleBlockDraw;
import nidefawl.qubes.render.gui.SingleBlockRenderer;
import nidefawl.qubes.render.impl.gl.ShadowRendererGL;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.SunLightModel;

public class Engine {
    public static boolean isVulkan;
    public static boolean isVulkanTodo;
    public static VKContext vkContext;
    public final static int NUM_PROJECTIONS    = 3 + 1;   // 3 sun view shadow pass + player view camera
    public final static int MAX_LIGHTS       = 1024;
    public final static RenderSettings RENDER_SETTINGS = new RenderSettings();
    public final static BlockPos GLOBAL_OFFSET = new BlockPos();
    private final static BlockPos LAST_REPOS = new BlockPos();
    private static Map<String, Integer> bufferBindingPoints = Maps.newHashMap();
    private static int NEXT_BUFFER_BINDING_POINT = 0;
    public static boolean INVERSE_Z_BUFFER = false;
    public static boolean OGL_INVERSE_Y = false;
    public static boolean isInverseZ = false;
    static ArrayList<TrueTypeFont> newfonts = new ArrayList<>();
    static ArrayList<TrueTypeFont> fonts = new ArrayList<>();

    private static IntBuffer   viewportBuf;
    private static FloatBuffer position;
    private static IntBuffer   allocBuffer;

    private static BufferedMatrix projection;
    private static BufferedMatrix _projection;
    private static BufferedMatrix view;
    private static BufferedMatrix viewInvYZ;
    private static BufferedMatrix viewprojection;
    private static BufferedMatrix modelviewprojection;
    private static BufferedMatrix matReproject;
    private static BufferedMatrix modelview;
    private static BufferedMatrix modelmatrix;
    private static BufferedMatrix normalMatrix;
    private static BufferedMatrix orthoP;
    private static BufferedMatrix orthoMV;
    private static BufferedMatrix orthoMVP;
    private static BufferedMatrix ortho3DP;
    private static BufferedMatrix ortho3DMV;
    private static BufferedMatrix tempMatrix;
    private static BufferedMatrix tempMatrix2;
    private static BufferedMatrix identity;
    private static BufferedMatrix modelviewprojectionUnjittered;
    public final static Matrix4f prevModel = new Matrix4f();
    public final static Matrix4f prevView = new Matrix4f();
    private static int TEMPORAL_IDX = 0;
    public static boolean TEMPORAL_OFFSET = false;
    public static Vector3f       pxOffset = new Vector3f(0,0, 0);
    public final static TransformStack pxStack = new TransformStack();
    public final static Matrix4f invertYZ = new Matrix4f().scale(1, -1, -1);
    

    public static FrameBuffer fbScene;
    public static float znear = 0.1f;
    public static float zfar = 1024F;

    static ITessState[] fullscreenquads;
    static ITessState quad;

    public static Frustum        camFrustum;
    public static Vector3f       lightPosition;
    public static Vector3f       lightDirection;
    public static float          sunAngle     = 0F;
    public static Camera         camera;
    public static Renders renders;
    public static MeshThread     regionRenderThread;
    
    public static RenderersVulkan rendersVK;
    public static RenderersGL rendersGL;

    public static ShadowProjector shadowProj;
    public static WorldRenderer  worldRenderer;
    public static SkyRenderer  skyRenderer;
    public static ShadowRenderer  shadowRenderer;
    public static BlurRenderer   blurRenderer;
    public static FinalRenderer  outRenderer;
    public static RegionRenderer regionRenderer;
    public static CubeParticleRenderer  particleRenderer;
    public static LightCompute  lightCompute;
    public static QModelBatchedRender renderBatched;
    private static float         aspectRatio;
    private static int           fieldOfView;

    public static boolean renderWireFrame = false;
    private static boolean isDepthMask = true;

    public static boolean isScissors = false;
    public static boolean isBlend = false;

    public static boolean updateRenderOffset;
    public final static SingleBlockRenderer blockRender = new SingleBlockRenderer();
    public final static SingleBlockDraw blockDraw = new SingleBlockDraw();
    public final static ItemRenderer itemRender = new ItemRenderer();
    static GLVAO active = null;
    final static int[] viewport = new int[] {0,0,0,0};
    public final static ShaderBuffer        debugOutput         = new ShaderBuffer("DebugOutputBuffer").setSize(4096*4);
    
    public final static SunLightModel sunlightmodel = new SunLightModel();
    private final static ReallocIntBuffer[] buffers = new ReallocIntBuffer[4];

    public static boolean userSettingUseBindless=true;
    static boolean isVAOSupportingBindless=false;
    static boolean clientStateBindlessElement=false;
    static boolean clientStateBindlessAttrib=false;
    public static boolean isDither=true;
    public static float Y_SIGN = 1.0f;
    public static VkDescriptor descriptorSetUboScene;
    public static VkDescriptor descriptorSetUboConstants;
    private static LongBuffer pDescriptorSets;
    private static VkDescriptor[] boundDescriptorSets = new VkDescriptor[4];
    private static IntBuffer pOffsets;
    private static VkRenderPassBeginInfo renderPassBeginInfo;
    private static VkExtent2D renderAreaExtent;
    private static VkOffset2D renderAreaOffset;
    private static VkCommandBufferBeginInfo cmdBufInfo;
    private static VkClearAttachment.Buffer clearAtt;
    private static VkClearRect.Buffer clearRect;
    private static VkExtent2D clearRect2DExtent;
    private static VkClearDepthStencilValue clearValue;
    
    public static void bindVAO(GLVAO vao) {
        bindVAO(vao, userSettingUseBindless);
    }
    public static void bindVAO(GLVAO vao, boolean bindless) {
        if (active != vao) {
            disableBindless();
            active = vao;
            if (active != null) {
                isVAOSupportingBindless = bindless&&vao.isBindless();
                GL30.glBindVertexArray(isVAOSupportingBindless ? active.vaoIdBindless : active.vaoId);
            } else {
                isVAOSupportingBindless = false;
                GL30.glBindVertexArray(0);
            }
        }
    }
    public static void enableBindless() {
        if (!clientStateBindlessElement) {
            clientStateBindlessElement = true;
            GL11.glEnableClientState(GL_ELEMENT_ARRAY_UNIFIED_NV);
        }
        if (!clientStateBindlessAttrib) {
            clientStateBindlessAttrib = true;
            GL11.glEnableClientState(GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV);
        }
    }
    public static void disableBindless() {
        if (clientStateBindlessElement) {
            clientStateBindlessElement = false;
            GL11.glDisableClientState(GL_ELEMENT_ARRAY_UNIFIED_NV);
        }
        if (clientStateBindlessAttrib) {
            clientStateBindlessAttrib = false;
            GL11.glDisableClientState(GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV);
        }
    }

    public static void bindIndexBuffer(GLVBO vbo) {
        if (isVAOSupportingBindless && !vbo.canUseBindless)
        {
            throw new GameError("Invalid state isVAOSupportingBindless && !vbo.canUseBindless");
        }
        if (isVAOSupportingBindless && vbo.canUseBindless) {
            enableBindless();
            glBufferAddressRangeNV(GL_ELEMENT_ARRAY_ADDRESS_NV, 0, vbo.addr, vbo.size);
//            System.out.println("going bindless element index "+vbo.addr+"/"+vbo.size);
        } else {
            disableBindless();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vbo.getVboId());
        }
    }

    public static void bindBuffer(GLVBO vbo) {
        bindBuffer(vbo, 0, active.vertStride);
    }
    public static void bindBuffer(GLVBO vbo, int bindingPoint, int stride) {
        if (isVAOSupportingBindless && !vbo.canUseBindless)
        {
            throw new GameError("Invalid state isVAOSupportingBindless && !vbo.canUseBindless");
        }
        if (isVAOSupportingBindless && vbo.canUseBindless) {
            enableBindless();
            for (int i = 0; i < active.list.size(); i++) {
                VertexAttrib attrib = active.list.get(i);
                glBufferAddressRangeNV(GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV, i, vbo.addr + attrib.offset*4, vbo.size - attrib.offset*4);
//                System.out.println(vbo.addr+"/"+vbo.size+" - "+attrib.offset);
            }
//          System.out.println("Not bindless :(");
//            System.out.println("going bindless attrib array "+vbo.addr+"/"+vbo.size);
        } else {
            disableBindless();
            GL43.glBindVertexBuffer(bindingPoint, vbo.getVboId(), 0, stride);
        }
    }
    

    public static void generateLightMapTexture() {

    }

    public static boolean checkGLError(String s) {
        if (!isVulkan) {
            int i = GL11.glGetError();
            if (i != 0) {
                String s1 = GameBase.getGlErrorString(i);
                throw new GameError("Error - " + s + ": " + s1);
            }
        }
        return false;
    }

    public static void baseInit() {
        viewportBuf = Memory.createIntBufferHeap(16);
        position = Memory.createFloatBufferHeap(3);
        allocBuffer = Memory.createIntBuffer(8);
        projection = new BufferedMatrix();
        _projection = new BufferedMatrix();
        view = new BufferedMatrix();
        viewInvYZ = new BufferedMatrix();
        viewprojection = new BufferedMatrix();
        modelview = new BufferedMatrix();
        modelmatrix = new BufferedMatrix();
        modelviewprojection = new BufferedMatrix();
        modelviewprojectionUnjittered = new BufferedMatrix();
        matReproject = new BufferedMatrix();
        normalMatrix = new BufferedMatrix();
        orthoP = new BufferedMatrix();
        orthoMV = new BufferedMatrix();
        orthoMVP = new BufferedMatrix();
        ortho3DP = new BufferedMatrix();
        ortho3DMV = new BufferedMatrix();
        tempMatrix = new BufferedMatrix();
        tempMatrix2 = new BufferedMatrix();
        identity = new BufferedMatrix();
        identity.setIdentity();
        identity.update();
        identity.update();
        camFrustum = new Frustum();
        lightPosition = new Vector3f();
        lightDirection = new Vector3f();
        camera = new Camera();
        sunlightmodel.setDayLen(10000);
        sunlightmodel.setTime(7500);
        System.out.println("Engine.baseinit: "+GameContext.getTimeSinceStart());
        if (isVulkan) {
            cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
            pDescriptorSets = memAllocLong(3);
            pOffsets = memAllocInt(32);
            renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .pNext(NULL)
                    .renderPass(0);
            VkRect2D renderArea = renderPassBeginInfo.renderArea();
            renderAreaExtent = renderArea.extent();
            renderAreaOffset = renderArea.offset();
            renderAreaOffset.x(0).y(0);
            clearAtt = VkClearAttachment.calloc(1);
            clearRect = VkClearRect.calloc(1);
            clearRect2DExtent = clearRect.rect().extent();
            clearValue = clearAtt.clearValue().depthStencil();
        }
    }

    public static void init(int w, int h) {
        init(EngineInitSettings.INIT_NONE.setFBSize(w, h));
    }

    public static void setZBufferSetting() {
        if (INVERSE_Z_BUFFER) {
            isInverseZ = true;
            glClearDepth(0.0f);
            if (GL.isClipControlSupported()) {
                ARBClipControl.glClipControl(OGL_INVERSE_Y?ARBClipControl.GL_UPPER_LEFT:ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
            } else {
                NVDepthBufferFloat.glDepthRangedNV(-1.0, 1.0);
            }
            glDepthFunc(GL_GEQUAL);
        }
    }

    public static void restoreZBufferSetting() {
        if (INVERSE_Z_BUFFER) {
            isInverseZ = false;
            glClearDepth(1.0f);
            if (GL.isClipControlSupported()) {
                ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
            } else {
                NVDepthBufferFloat.glDepthRangedNV(0.0, 1.0);
            }
            glDepthFunc(GL_LEQUAL);
        }
    }
    public static void init(EngineInitSettings init) {
        guiWidth = displayWidth = init.fbWidth;
        guiHeight = displayHeight = init.fbHeight;
        isVulkanTodo = isVulkan = init.isVulkan;
        INVERSE_Z_BUFFER = init.inverseZBuffer;
        OGL_INVERSE_Y = init.inverseClipspaceYOpengl;
        if (!INVERSE_Z_BUFFER && OGL_INVERSE_Y) {
            throw new IllegalArgumentException("cannot reverse y without z");
        }
        if (isVulkan||OGL_INVERSE_Y) {
            Y_SIGN = -1.0f;
        }
        isBlend = true;
        if (!isVulkan) {
            glActiveTexture(GL_TEXTURE0);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glActiveTexture(GL_TEXTURE0)");
            for (int i = 1; i < 4; i++) {
                GL30.glDisablei(GL_BLEND, i);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("GL30.glDisablei(GL_BLEND, "+i+")");
            }
            GL30.glEnablei(GL_BLEND, 0);
        } else {
        }

        
        baseInit();
        GLVAO.initVAOs(isVulkan);
        if (!isVulkan) {
            UniformBuffer.init();
            Shader.init();   
        } else {
            VkRenderPasses.initClearValues(INVERSE_Z_BUFFER);
            VkTess.init(vkContext, 32);
            UniformBuffer.init(vkContext, 32);
            VkPipelines.init(vkContext);
            descriptorSetUboScene = vkContext.descLayouts.allocDescSetUBOScene();
            descriptorSetUboScene.setBindingBuffer(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, UniformBuffer.uboMatrix3D.getDescriptorBuffer());
            descriptorSetUboScene.setBindingBuffer(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, UniformBuffer.uboMatrix2D.getDescriptorBuffer());
            descriptorSetUboScene.setBindingBuffer(2, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, UniformBuffer.uboSceneData.getDescriptorBuffer());
            descriptorSetUboScene.setBindingBuffer(3, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, UniformBuffer.uboMatrixShadow.getDescriptorBuffer());
            descriptorSetUboScene.update(vkContext);
            descriptorSetUboConstants = vkContext.descLayouts.allocDescSetUBOConstants();
            descriptorSetUboConstants.setBindingBuffer(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, UniformBuffer.VertexDirections.getDescriptorBuffer());
            descriptorSetUboConstants.setBindingBuffer(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, UniformBuffer.TBNMat.getDescriptorBuffer());
            descriptorSetUboConstants.update(vkContext);
        }

        flushRenderTasks();
        if (isVulkan) {
            renders = rendersVK = new RenderersVulkan(vkContext, init);
        } else {
            renders = rendersGL = new RenderersGL(init);
        }
        if (regionRenderer != null) {
            regionRenderThread = new MeshThread(3);
            regionRenderThread.init();
            regionRenderer.reRender();
        }
//        if (initRenderers) {
//            flushRenderTasks();
//            registerRenderers();
//            if (regionRenderer != null) {
//                regionRenderThread = new MeshThread(3);
//                regionRenderThread.init();
//                regionRenderer.reRender();
//            }
//        } else {
//            Shaders.init();
//            ShaderBuffer.init();
//        }
        blockDraw.init();
        itemRender.init();
        pxStack.setCallBack(new StackChangeCallBack() {
            @Override
            public void onChange(Vector3f vec) {
                pxOffset.set(vec);
                UniformBuffer.updatePxOffset();
                if (Engine.isVulkan) {
                    pOffsets.put(2, UniformBuffer.uboSceneData.getDynamicOffset());
                    Engine.rebindSceneDescriptorSet();
                }
            }
        });
        System.out.println("Engine.init @"+GameContext.getTimeSinceStart()+"ms");
    }

    public static void resize(int displayWidth, int displayHeight) {
        resizeProjection(displayWidth, displayHeight);
        resizeShadowProjection(displayWidth, displayHeight);
        resizeRenderers(displayWidth, displayHeight);
    }

    public static void resizeShadowProjection(int displayWidth, int displayHeight) {
        if (shadowProj != null) {
            shadowProj.updateProjection(znear, zfar, aspectRatio, fieldOfView);
        }
    }
    /**
     * Resizes 3d projection matrix, 2d orthogonal projection for gui, 2D fullscreen quad.
     * Fast - can be called each frame.
     * @param displayWidth
     * @param displayHeight
     */
    public static void resizeProjection(int displayWidth, int displayHeight) {
        boolean resChanged = viewportBuf.get(2)!=displayWidth||viewportBuf.get(3)!=displayHeight;
        if (!resChanged)
            return;
        fieldOfView = 70;
        aspectRatio = (float) displayWidth / (float) displayHeight;
        znear = 0.01F;
        zfar = INVERSE_Z_BUFFER?3000F:1024F;
        viewportBuf.position(0);
        viewportBuf.put(0);
        viewportBuf.put(0);
        viewportBuf.put(displayWidth);
        viewportBuf.put(displayHeight);
        viewportBuf.flip();
        if (isVulkan) {
            if (!INVERSE_Z_BUFFER) {
                Project.fovProjMatVk(fieldOfView, aspectRatio, znear, zfar, _projection);
            } else {
                Project.fovProjMatInfInvZVk(fieldOfView, aspectRatio, znear, _projection);
            }
        } else {

            if (!INVERSE_Z_BUFFER) {
                Project.fovProjMat(fieldOfView, aspectRatio, znear, zfar, _projection);
            } else {
                Project.fovProjMatInfInvZ(fieldOfView, aspectRatio, znear, _projection);
            }
            if (OGL_INVERSE_Y) {
                _projection.m11 = -_projection.m11;
            }
        }
        
        camFrustum.setCamInternals(fieldOfView, aspectRatio, znear, zfar);
        _projection.update();
        _projection.update();
        projection.load(_projection);
        projection.update();


        updateOrthoMatrix(displayWidth, displayHeight);
        //TODO: figure out something better for VR
        // Don't change the fullscreen quad buffer and matrix so often
        // The viewports for VR/window/gui/scene are constant until window resize/HMD change
        // Allocate several resources for them so we can use device local memory for vulkan
        // Right now the device local uploads only work in preRenderUpdate, thats why we use host memory for the quads below on vulkan
        if (fullscreenquads == null) {
            fullscreenquads = new ITessState[4];
            for (int i = 0; i < fullscreenquads.length; i++)
                fullscreenquads[i] = isVulkan ? new VkTesselatorState(vkContext, true) : new TesselatorState(GL15.GL_STATIC_DRAW);
        }
        if (quad == null) {
            quad = isVulkan ? new VkTesselatorState(vkContext, true) : new TesselatorState(GL15.GL_STATIC_DRAW);
        }
        ITess tess = getTess();
        tess.resetState();
        int tw = displayWidth;
        int th = displayHeight;
        float x = 0;
        float y = 0;
        tess.resetState();
        //Draw some quads with fullscreen resultions, 2 different windings, flipped/non-flipped y texcoord
        tess.setColor(0xFFFFFF, 0xff);
        tess.add(x + tw,   y,      0, 1, 1);
        tess.add(x,        y,      0, 0, 1);
        tess.add(x,        y + th, 0, 0, 0);
        tess.add(x + tw,   y + th, 0, 1, 0);
        tess.drawQuads(fullscreenquads[0]); // == Engine.drawFullscreenQuad
        tess.resetState();
        tess.setColor(0xFFFFFF, 0xff);
        tess.add(x + tw,   y,      0, 1, 0);
        tess.add(x,        y,      0, 0, 0);
        tess.add(x,        y + th, 0, 0, 1);
        tess.add(x + tw,   y + th, 0, 1, 1);
        tess.drawQuads(fullscreenquads[1]);
        tess.resetState();
        tess.setColor(0xFFFFFF, 0xff);
        tess.add(x + tw,   y + th, 0, 1, 1);
        tess.add(x,        y + th, 0, 0, 1);
        tess.add(x,        y,      0, 0, 0);
        tess.add(x + tw,   y,      0, 1, 0);
        tess.drawQuads(fullscreenquads[2]);
        tess.resetState();
        tess.setColor(0xFFFFFF, 0xff);
        tess.add(x + tw,   y + th, 0, 1, 0);
        tess.add(x,        y + th, 0, 0, 0);
        tess.add(x,        y,      0, 0, 1);
        tess.add(x + tw,   y,      0, 1, 1);
        tess.drawQuads(fullscreenquads[3]);
        tess.resetState();
        if (!isVulkan)
            tess.setColor(0xFFFFFF, 0xff);
        tess.add(1, 0, 0, 1, 0);
        tess.add(0, 0, 0, 0, 0);
        tess.add(0, 1, 0, 0, 1);
        tess.add(1, 1, 0, 1, 1);
        tess.drawQuads(quad);
        tess.resetState();

    }

    /**
     * Resizes renderers framebuffers, may reload shaders
     * Slow - cannot be called each frame
     * @param displayWidth
     * @param displayHeight
     */
    public static void resizeRenderers(int displayWidth, int displayHeight) {
        renders.resizeRenderers(displayWidth, displayHeight);
        if (!isVulkan) {
            UniformBuffer.rebindShaders(); // For some stupid reason we have to rebind
            ShaderBuffer.rebindShaders();
        }
    }
    public static void updateOrthoMatrix(float displayWidth, float displayHeight) {
        updateOrthoMatrix(displayWidth, displayHeight, OGL_INVERSE_Y || isVulkan);
    }
    public static void updateOrthoMatrix(float displayWidth, float displayHeight, boolean flipY) {
        orthoMV.setIdentity();
        orthoMV.update();
        orthoP.setZero();
        Project.orthoMat(0, displayWidth, flipY?displayHeight:0, flipY?0:displayHeight, -4200, 200, orthoP);
//        if (flipY) {
//            orthoP.m11*=-1.0;
//            orthoP.m31*=-1.0;
//        }
        orthoP.update();
        Matrix4f.mul(orthoP, orthoMV, orthoMVP);
        orthoMVP.update();
        
        ortho3DMV.setIdentity();
        ortho3DMV.update();
        
        ortho3DP.setZero();
        Project.orthoMat(0, displayWidth, flipY?displayHeight:0, flipY?0:displayHeight, -400, 400, ortho3DP);
//        if (flipY) {
//            ortho3DP.m11*=-1.0;
//            ortho3DP.m31*=-1.0;
//        }
        ortho3DP.update();
    }

    public static void drawFullscreenQuad() {
        fullscreenquads[isVulkan?1:0].drawQuads();
    }
    public static void drawFSQuad(int n) {
        fullscreenquads[n].drawQuads();
    }
    public static void drawFSTri() {
        bindVAO(GLVAO.vaoEmpty);
        glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    public static void drawQuad() {
        quad.drawQuads();
    }


    public static BufferedMatrix getMatSceneP_internal() {
        return _projection;
    }
    public static BufferedMatrix getMatSceneP() {
        return projection;
    }
    public static BufferedMatrix getMatSceneV() {
        return view;
    }
    public static BufferedMatrix getMatSceneV_YZ_Inv() {
        return viewInvYZ;
    }
    public static BufferedMatrix getMatSceneVP() {
        return viewprojection;
    }

    public static BufferedMatrix getMatSceneMV() {
        return modelview;
    }
    
    public static BufferedMatrix getMatSceneM() {
        return modelmatrix;
    }

    public static BufferedMatrix getMatSceneMVP() {
        return modelviewprojection;
    }
    public static BufferedMatrix getMatSceneMVPUnjittered() {
        return modelviewprojectionUnjittered;
    }
    public static BufferedMatrix getMatReproject() {
        return matReproject;
    }

    public static BufferedMatrix getMatSceneNormal() {
        return normalMatrix;
    }
    
    public static BufferedMatrix getMatOrthoP() {
        return orthoP;
    }
    public static BufferedMatrix getMatOrthoMV() {
        return orthoMV;
    }
    public static BufferedMatrix getMatOrthoMVP() {
        return orthoMVP;
    }
    public static BufferedMatrix getMatOrtho3DMV() {
        return ortho3DMV;
    }

    public static BufferedMatrix getMatOrtho3DP() {
        return ortho3DP;
    }

    public static BufferedMatrix getTempMatrix() {
        return tempMatrix;
    }

    public static BufferedMatrix getTempMatrix2() {
        return tempMatrix2;
    }
    public static BufferedMatrix getIdentityMatrix() {
        return identity;
    }

    public static void updateCamera() {
        updateCamera(camera);
    }
    public static void updateCamera(Camera camera) {
        Matrix4f camView = camera.getViewMatrix();
        Vector3f camPos = camera.getPosition();
        setTemporalIdx(TEMPORAL_IDX+1);
        updateCamera(camView, camPos);
        prevView.load(view);
        prevModel.load(modelmatrix);
    }
    public static int getTemporalJitterIdx() {
        return TEMPORAL_IDX;
    }
    public static void updateFrustumFromInternal() {
        camFrustum.setPos(camera.getPosition(), view.getInvMat4());
        camFrustum.set(modelviewprojection);
    }

    public static void setFrustum(Matrix4f mvp, Matrix4f viewInv, Vector3f cameraPosition) {
        camFrustum.setPos(cameraPosition, viewInv);
        camFrustum.set(mvp);
    }
    public static void updateGlobalRenderOffset(Vector3f camPos) {
        updateRenderOffset = updateGlobalRenderOffset(camPos.x, camPos.y, camPos.z);
    }
    public static Matrix4f composeView(boolean addShake, Matrix4f out) {
        Matrix4f tempView = Matrix4f.pool();
        camera.calcViewMatrix(tempView, addShake);
        if (out == null)
            return tempView;
        out.load(tempView);
        return out;
    }
    
    public static Matrix4f composeModelMatrix(boolean addCameraPos, boolean addGlobalOffset, Matrix4f out) {
        Matrix4f tempView = Matrix4f.poolIdentity(); 
        if (addCameraPos) {
            Vector3f cam = camera.getPosition();
            tempView.m30 -= cam.x;
            tempView.m31 -= cam.y;
            tempView.m32 -= cam.z;
        }
        if (addGlobalOffset) {
            tempView.m30 -= GLOBAL_OFFSET.x;
            tempView.m32 -= GLOBAL_OFFSET.x;
        }
        if (out == null)
            return tempView;
        out.load(tempView);
        return out;
    }
    public static Matrix4f composeModelView(boolean addCameraPos, boolean addGlobalOffset, boolean addShake, Matrix4f out) {
        Matrix4f tempView = Matrix4f.pool();
        camera.calcViewMatrix(tempView, addShake);
        if (addCameraPos) {
            Vector3f cam = camera.getPosition();
            tempView.m30 -= cam.x;
            tempView.m31 -= cam.y;
            tempView.m32 -= cam.z;
        }
        if (addGlobalOffset) {
            tempView.m30 -= GLOBAL_OFFSET.x;
            tempView.m32 -= GLOBAL_OFFSET.x;
        }
        if (out == null)
            return tempView;
        out.load(tempView);
        return out;
    }
    public static void setTemporalIdx(int i) {
        TEMPORAL_IDX = i%2;
    }
    private static void updateCamera(Matrix4f camView, Vector3f camPos) {
        if (TEMPORAL_OFFSET) {
            addJitterToProjection(_projection, projection);
        } else {
            projection.load(_projection);
        }
        projection.update();
        view.load(camView);
        modelmatrix.setIdentity();
        modelmatrix.translate(-camPos.x, -camPos.y, -camPos.z);
        modelmatrix.translate(GLOBAL_OFFSET.x, 0, GLOBAL_OFFSET.z);
        _updateInternalMatrices();
        if (TEMPORAL_OFFSET) {
            Matrix4f prevVP = Matrix4f.pool();
            Matrix4f matTranslation = Matrix4f.pool();
            Matrix4f.mul(projection, prevView, prevVP);
            Matrix4f.mul(prevModel, modelmatrix.getInvMat4(), matTranslation);
            Matrix4f.mul(prevVP, matTranslation, prevVP);
            Matrix4f.mul(prevVP, viewprojection.getInvMat4(), matReproject);
            matReproject.update();
        }
    }
    public static void _updateInternalMatrices() {
        view.update();
        modelmatrix.update();
        viewInvYZ.load(view);
        viewInvYZ.mulMat(invertYZ);
        viewInvYZ.update();
        Matrix4f.mul(view, modelmatrix, modelview);
        modelview.update();
        Matrix4f.mul(_projection, modelview, modelviewprojectionUnjittered);
        Matrix4f.mul(projection, modelview, modelviewprojection);
        Matrix4f.mul(projection, view, viewprojection);
        viewprojection.update();
        modelviewprojection.update();
        modelviewprojectionUnjittered.update();
        normalMatrix.setIdentity();
        normalMatrix.invert().transpose();
        normalMatrix.update();
        
    }
    public static void addJitterToProjection(Matrix4f matIn, Matrix4f matOut) {
        float x = -0.25f;
        float y = 0.25f;
        if (TEMPORAL_IDX%2==1) {
            x*=-1;
            y*=-1;
        }
        float vw=2.0f / viewportBuf.get(2);
        float vh=2.0f / viewportBuf.get(3);
        Matrix4f tmpMat = Matrix4f.poolIdentity();
        tmpMat.translate(vw*x, vh*y, 0);
        Matrix4f.mul(tmpMat, matIn, matOut);
        
    }
    public static void setViewAndModelMatrix(Matrix4f v, Matrix4f m) {
        view.load(v);
        modelmatrix.load(m);
        _updateInternalMatrices();
        UniformBuffer.updateSceneMatrices();
    }
    public static void setViewMatrixCameraPos(Matrix4f v, Vector3f cameraPos) {
        view.load(v);
        modelmatrix.setIdentity();
        modelmatrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        modelmatrix.translate(GLOBAL_OFFSET.x, 0, GLOBAL_OFFSET.z);
        _updateInternalMatrices();
        UniformBuffer.updateSceneMatrices();
    }
    public static void setModelMatrix(Matrix4f m) {
        modelmatrix.load(m);
        _updateInternalMatrices();
        UniformBuffer.updateSceneMatrices();
    }
    public static void setViewMatrix(Matrix4f v) {
        view.load(v);
        _updateInternalMatrices();
        UniformBuffer.updateSceneMatrices();
    }

    
    private static boolean updateGlobalRenderOffset(float x, float y, float z) {
        final int OFFSET_BITS = 7;
        final int OFFSET_REPOS_DIST = 1024;
        int ix = GameMath.floor(x);
        int iz = GameMath.floor(z);
//        int dbgX = -(3<<OFFSET_BITS);
//        if (1==1){
//            boolean update = !GLOBAL_OFFSET.isEqualTo(dbgX,0,0);
//            GLOBAL_OFFSET.set(dbgX, 0, 0);
//            return update;
//        }
        final int distSq3D = GameMath.distSq3Di(ix, 0, iz, LAST_REPOS.x, 0, LAST_REPOS.z);
        if (distSq3D < 0/*INTEGER OVERFLOW*/ || distSq3D > OFFSET_REPOS_DIST*OFFSET_REPOS_DIST) {
            int offX = (ix>>OFFSET_BITS<<OFFSET_BITS);
            int offZ = (iz>>OFFSET_BITS<<OFFSET_BITS);
            boolean update = !GLOBAL_OFFSET.isEqualTo(offX, 0, offZ);
            GLOBAL_OFFSET.set(offX, 0, offZ);
//            GLOBAL_OFFSET.set(0, 0, 0);
            LAST_REPOS.set(ix, 0, iz);
            if (update)
                System.out.println("REPOS "+GLOBAL_OFFSET);
            return update;
        }
        return false;
    }

    public static FrameBuffer getSceneFB() {
        return fbScene;
    }



    public static void flushRenderTasks() {
        if (regionRenderThread != null) {
            regionRenderThread.flush();
            while (regionRenderThread.hasTasks() && regionRenderThread.isRunning()) {
                regionRenderThread.finishTask();
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void restartRenderThreads() {
        if (regionRenderThread != null) {
            while (regionRenderThread.hasTasks()) {
                regionRenderThread.finishTask();
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            regionRenderThread.stopThread();
            regionRenderThread = null;
        }
        regionRenderThread = new MeshThread(3);
        regionRenderThread.init();
    }


    public static void setLightPosition(Vector3f v) {
        lightPosition.set(v);
        lightDirection.set(v);
        lightDirection.normalise();
    }
    public static void updateShadowProjections(float fTime) {
//        Engine.worldRenderer.debugBBs.clear();
        shadowProj.calcSplits(modelview, lightDirection, getShadowMapTextureSize() / 2.0f); //divide tex size by 2 as we use only a quarter per cascade

        
    }

    public static void stop() {
        if(GameBase.DEBUG_LAYER) System.err.println("Engine.stop");
        if (renders != null) {
            renders.release();
        }
        if (regionRenderThread != null) {
            regionRenderThread.stopThread();
            regionRenderThread.cleanup();
        }
        UniformBuffer.destroy();
        GLVAO.destroy();
        if (isVulkan) {
            for (int i = 0; i < fonts.size(); i++) {
                fonts.get(i).destroy(vkContext);
            }
            fonts.clear();
            memFree(pDescriptorSets);
            memFree(pOffsets);
            renderPassBeginInfo.free();
            cmdBufInfo.free();
            VkPipelines.destroyShutdown(vkContext);
            VkRenderPasses.destroyShutdown(vkContext);
        }
    }



    public static void unprojectScreenSpace(float winX, float winY, float screenZ, float rW, float rH, Vector3f out) {
        float screenX = (winX / rW) * 2.0f - 1.0f;
        float screenY = (winY / rH) * 2.0f - 1.0f;
        if (!isVulkan && !isInverseZ)
            screenZ = (screenZ) * 2.0f - 1.0f;
        else {
            screenZ *= 0.99f;
        }
        out.set(screenX, screenY, screenZ);
        Matrix4f.transform(getMatSceneMVPUnjittered().getInvMat4(), out, out);
        out.add(Engine.GLOBAL_OFFSET);
    }
    public static void unprojectScreenSpaceOld(float winX, float winY, float screenZ, float rW, float rH, Vector3f out) {
        if (!Project.gluUnProject(winX, winY, screenZ, getMatSceneMV().get(), getMatSceneP().get(), viewportBuf, position)) {
            System.err.println("unproject fail 1");
        }
        out.load(position);
        position.clear();
    }


    public static void setSceneFB(FrameBuffer fb) {
        fbScene = fb;
    }

    public static IntBuffer glGenBuffers(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("i < 1");
        }
        allocBuffer.clear();
        allocBuffer.position(0);
        allocBuffer.limit(i);
        GL15.glGenBuffers(allocBuffer);
        allocBuffer.position(0);
        allocBuffer.limit(i);
        return allocBuffer;
    }

    public static void deleteBuffers(int ...buffers) {
        allocBuffer.clear();
        allocBuffer.put(buffers);
        allocBuffer.flip();
        GL15.glDeleteBuffers(allocBuffer);
    }


    public static void toggleWireFrame() {
        renderWireFrame = !renderWireFrame;
        worldRenderer.initShaders();
    }



    public static void toggleDrawMode() {
        worldRenderer.initShaders();
    }

    /**
     * @param mat2
     */
    public static void setOrthoMV(BufferedMatrix mv) {
        Matrix4f.mul(orthoP, mv, orthoMVP);
        orthoMVP.update();
        UniformBuffer.updateOrtho();
    }

    /**
     * 
     */
    public static void restoreOrtho() {
        Matrix4f.mul(orthoP, orthoMV, orthoMVP);
        orthoMVP.update();
        UniformBuffer.updateOrtho();
    }

    public static Vector3f getPxOffset() {
        return pxOffset;
    }

    public static int getBindingPoint(String name) {
        Integer i = bufferBindingPoints.get(name);
        if (i == null) {
            i = NEXT_BUFFER_BINDING_POINT;
            bufferBindingPoints.put(name, NEXT_BUFFER_BINDING_POINT++);
        }
        return i;
    }

    public static void enableDepthMask(boolean flag) {
        isDepthMask = flag;
        if (!isVulkan)
            GL11.glDepthMask(flag);
    }

    public static void enableScissors() {
        if (isVulkan) {
            if (curPipeline.pipelineScissors == VK_NULL_HANDLE) {
                throw new GameLogicError("Cant enable scissor for current");
            }
            vkCmdBindPipeline(curCommandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, curPipeline.pipelineScissors);
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
        isScissors = true;
    }

    public static void disableScissors() {
        if (isVulkan) {
//            System.out.println("back to normal pipeline");
            vkCmdBindPipeline(curCommandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, curPipeline.pipeline);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        isScissors = false;
    }
    public static void setOverrideScissorTest(boolean b) {
        if (!b) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);    
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
    public static void restoreScissorTest() {
        if (!isScissors) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);    
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
    public static boolean isDepthMask() {
        return isDepthMask;
    }
    public static boolean isBlend() {
        return isBlend;
    }
    public static void restoreDepthMask() {
        GL11.glDepthMask(isDepthMask);
    }
    //May need stack sometime
    public static void setOverrideDepthMask(boolean b) {
        GL11.glDepthMask(b);
    }
    public static void setBlend(boolean b) {
        if (isBlend == b) {
            return;
        }
        if (b) {
            GL30.glEnablei(GL_BLEND, 0);
        } else {
            GL30.glDisablei(GL_BLEND, 0);
        }
        isBlend = b;
    }

    public static void setViewport(int x, int y, int w, int h) {
        if (viewport[0] != x || viewport[1] != y || viewport[2] != w || viewport[3] != h) {
            viewport[0] = x;
            viewport[1] = y;
            viewport[2] = w;
            viewport[3] = h;
            if (!isVulkan) {

                GL11.glViewport(x, y, w, h);
            }
//            System.out.println(Stats.fpsCounter + ", "+w+","+h);
//            Thread.dumpStack();
        }
    }
    public static int[] getViewport() {
        return viewport;
    }

    public static void setDefaultViewport() {
        setViewport(0, 0, displayWidth, displayHeight);
    }

    public static SunLightModel getSunLightModel() {
        return sunlightmodel;
    }

    public static ReallocIntBuffer getIntBuffer() {
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] == null) {
                buffers[i] = new ReallocIntBuffer();
                buffers[i].setInUse(true);
                return buffers[i];
            }
            if (!buffers[i].isInUse()) {
                buffers[i].setInUse(true);
                return buffers[i];
            }
        }
        return null;
    }
    public static int getShadowMapTextureSize() {
        if (shadowRenderer != null)
            return shadowRenderer.getTextureSize();
        return 1024*2;
    }
    public static int getShadowDepthTex() {
        if (shadowRenderer instanceof ShadowRendererGL)
            return ((ShadowRendererGL) shadowRenderer).getDepthTex();
        return TMgr.getEmptyWhite();
    }
    public static int getLightTexture() {
        if (Engine.lightCompute != null)
            return RenderersGL.lightCompute.getTexture();
        return TMgr.getEmpty();
    }
    public static int getAOTexture() {
        if (RENDER_SETTINGS.ao > 0) {
            return RenderersGL.outRenderer.fbSSAO.getTexture(0);
        }
        return TMgr.getEmptyWhite();
    }
    public static boolean getRenderVelocityBuffer() {
        if (outRenderer != null)
            return Engine.RENDER_SETTINGS.smaaMode == 2;
        return false;
    }
    public static boolean getRenderMaterialBuffer() {
        if (outRenderer != null)
            return Engine.RENDER_SETTINGS.smaaMode > 0 && Engine.RENDER_SETTINGS.smaaPredication;
        return false;
    }
    public static int setDepthFunc(int glCompareFunc) {
        if (isInverseZ) {
        switch (glCompareFunc) {
            case GL_GREATER:
                glCompareFunc = GL_LESS;
                break;
            case GL_GEQUAL:
                glCompareFunc = GL_LEQUAL;
                break;
            case GL_LEQUAL:
                glCompareFunc = GL_GEQUAL;
                break;
            case GL_LESS:
                glCompareFunc = GL_GREATER;
                break;
        }
        }
        GL11.glDepthFunc(glCompareFunc);
        return glCompareFunc;
    }
    public static ITess getFontTess() {
        if (isVulkan) {
            return VkTess.tessFont;
        }
        return Tess.tessFont;
    }
    public static ITess getTess() {
        if (isVulkan) {
            return VkTess.instance;
        }
        return Tess.instance;
    }
    
    public static void preRenderUpdateVK() {
        for (int i = 0; i < newfonts.size(); i++) {
            newfonts.get(i).setup(vkContext);
            fonts.add(newfonts.get(i));
        }
        newfonts.clear();
    }
    public static void registerTTF(TrueTypeFont trueTypeFont) {
        newfonts.add(trueTypeFont);
    }

    public static void setPipeStateGUI() {
        if (!isVulkan) {
            Shaders.gui.enable();
        } else {
            Engine.clearDescriptorSets();
            Engine.bindPipeline(VkPipelines.gui);
        }
        
    }
    public static void setPipeStateFontrenderer() {
        if (!isVulkan) {
            Shaders.textured.enable();
        } else {
            Engine.bindPipeline(VkPipelines.fontRender2D);
        }
    }
    
    public static void setPipeStateColored2D() {
        if (!isVulkan) {
            Shaders.colored.enable();
        } else {
            Engine.clearDescriptorSets();
            Engine.bindPipeline(VkPipelines.colored2D);
        }
    }
    
    public static void setPipeStateTextured2D() {
        if (!isVulkan) {
            Shaders.textured.enable();
        } else {
            Engine.bindPipeline(VkPipelines.textured2d);
        }
    }

    
    //single threaded!
    static VkCommandBuffer curCommandBuffer;
    static VkPipeline curPipeline;
    private static boolean rebindDescSet;
    private static VkRenderPass curPass;
    public static void bindPipeline(VkPipeline pipe) {
        if (curPipeline != pipe) {
            curPipeline = pipe;
            if (isScissors && pipe.pipelineScissors==VK_NULL_HANDLE) {
                System.err.println("MISSING SCISSORS PIPE VERSION");
            }
            vkCmdBindPipeline(curCommandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, 
                    isScissors ? (pipe.pipelineScissors) : (pipe.pipeline));
            if (isScissors)
                pxStack.vkUpdateLastScissors();
            rebindDescSet = true;
        }
        if (rebindDescSet) {
            rebindDescSet = false;
            rebindSceneDescriptorSet();
        }
    }
    public static void beginCommandBuffer(VkCommandBuffer commandBuffer) {
        curCommandBuffer = commandBuffer;
        int err = vkBeginCommandBuffer(commandBuffer, cmdBufInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
        }
    }
    public static void endCommandBuffer() {
        int err = vkEndCommandBuffer(curCommandBuffer);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to end render command buffer: " + VulkanErr.toString(err));
        }
    }

    public static void rebindSceneDescriptorSet() {
        if (curPipeline == null)
            return;
        pDescriptorSets.clear();
        for (int i = 0; i < boundDescriptorSets.length; i++) {
            if (boundDescriptorSets[i] != null) {
                pDescriptorSets.put(boundDescriptorSets[i].get());
            }            
        }
        
        pDescriptorSets.flip();
        pOffsets.put(0, UniformBuffer.uboMatrix3D.getDynamicOffset());
        pOffsets.put(1, UniformBuffer.uboMatrix2D.getDynamicOffset());
        pOffsets.put(2, UniformBuffer.uboSceneData.getDynamicOffset());
        pOffsets.put(3, UniformBuffer.uboMatrixShadow.getDynamicOffset());
        pOffsets.position(0).limit(4);
        vkCmdBindDescriptorSets(curCommandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, curPipeline.getLayoutHandle(), 0, 
                pDescriptorSets, pOffsets);
    }
    public static void beginRenderPass(VkRenderPass pass, long framebuffer, int flags) {
        curPipeline = null;
        curPass = pass;
        renderAreaExtent.set(viewport[2], viewport[3]);
        renderPassBeginInfo.renderPass(curPass.get());
        renderPassBeginInfo.framebuffer(framebuffer);
        renderPassBeginInfo.pClearValues(pass.clearValues);
        vkCmdBeginRenderPass(curCommandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
        boundDescriptorSets[0] = descriptorSetUboScene;
        rebindDescSet = true;//TODO: test + remove
    }
    public static void endRenderPass() {
        vkCmdEndRenderPass(curCommandBuffer);
        curPipeline = null;
        curPass = null;
        rebindDescSet = true;
    }
    public static void setDescriptorSet(int idx, VkDescriptor descriptorSet) {
        if (boundDescriptorSets[idx] != descriptorSet) {
            boundDescriptorSets[idx] = descriptorSet;
            rebindDescSet = true;
        }
    }
    public static void clearDescriptorSet(int idx) {
        rebindDescSet = boundDescriptorSets[idx] != null;
        boundDescriptorSets[idx] = null;
    }
    private static void clearDescriptorSets() {
        for (int i = 1; i < boundDescriptorSets.length; i++) {
            rebindDescSet |= boundDescriptorSets[i] != null;
            boundDescriptorSets[i] = null;
        }
    }
    public static VkCommandBuffer getDrawCmdBuffer() {
        return curCommandBuffer;
    }
    public static void lineWidth(float width) {
        if (!isVulkan) {
            GL11.glLineWidth(width);
        } else {
            vkCmdSetLineWidth(curCommandBuffer, width);
        }
    }
    public static int getGuiWidth() {
        return guiWidth;
    }
    public static int getGuiHeight() {
        return guiHeight;
    }

    public static int guiWidth;
    public static int guiHeight;
    public static int displayWidth;
    public static int displayHeight;
    public static void updateGuiResolution(int w, int h) {
        guiWidth = w;
        guiHeight = h;
    }
    public static void updateRenderResolution(int w, int h) {
        displayWidth = w;
        displayHeight = h;
    }
    public static int fbWidth() {
        return displayWidth;
    }
    public static int fbHeight() {
        return displayHeight;
    }
    public static ITessState newTessBuffer(boolean b) {
        if (isVulkan) {
            return new VkTesselatorState(vkContext);
        }
        return new TesselatorState(GL15.GL_DYNAMIC_DRAW);
    }
    public static float getMaxAnsitropic() {
        if (isVulkan) {
            if (vkContext.features.samplerAnisotropy())
            {
                return vkContext.limits.maxSamplerAnisotropy();
            }
            return 1.0f;
        }
        return glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
    }
    
    public static void clearDepth() {
        clearRect.get(0).layerCount(1).baseArrayLayer(0);
        clearRect2DExtent.width(displayWidth).height(displayHeight);
        clearAtt.get(0).aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT|VK_IMAGE_ASPECT_STENCIL_BIT);
        clearValue.set(0, 0);
        vkCmdClearAttachments(Engine.getDrawCmdBuffer(), clearAtt, clearRect);
    
    }
}
