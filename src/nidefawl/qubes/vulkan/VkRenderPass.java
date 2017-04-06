package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDescription.Buffer;

import nidefawl.qubes.gl.Engine;

public abstract class VkRenderPass {

    public final VkClearValue.Buffer renderPassClearValues = VkClearValue.calloc(8);
    public final VkClearValue.Buffer definedClearValues = VkClearValue.calloc(8);
    private VkClearDepthStencilValue definedClearValueDepth;
    private final VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(8);

    public int                            nAttachments;
    int                            nColorAttachments;
    public boolean                 hasDepthAttachement;
    public final int[]             attachmentType = new int[8];
    
    long                           renderPass;


    private String name;
    public VkRenderPass() {
        reset();
        VkRenderPasses.registerPass(this);
        this.name = getClass().getSimpleName().replaceFirst("VkRender", "");
    }
    
    void reset() {
        hasDepthAttachement = false;
        nAttachments = 0;
        nColorAttachments = 0;
        Arrays.fill(attachmentType, 0);
    }
    public void setClearValueColor(int idx, float r, float g, float b, float a) {
        definedClearValues.get(idx).color()
            .float32(0, r)
            .float32(1, g)
            .float32(2, b)
            .float32(3, a);
    }

     VkAttachmentDescription addColorAttachment(int idx, int colorFormat) {
        nColorAttachments++;
        nAttachments++;
        attachmentType[idx] = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        VkAttachmentDescription n = attachments.get(idx);
        n
          .format(colorFormat)
          .samples(VK_SAMPLE_COUNT_1_BIT)
          .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
          .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
          .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
          .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
          .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        return n;
    }
     VkAttachmentDescription addDepthAttachment(int idx, int depthFormat) {
        hasDepthAttachement = true;
        nAttachments++;
        attachmentType[idx] = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        VkAttachmentDescription n = attachments.get(idx);
        n.format(depthFormat)
          .samples(VK_SAMPLE_COUNT_1_BIT)
          .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
          .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
          .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
          .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
          .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        this.definedClearValueDepth = definedClearValues.get(idx).depthStencil();
        return n;
    }
    public void destroyRenderPass(VKContext ctxt) {
        if (this.renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(ctxt.device, this.renderPass, null);
            this.renderPass = VK_NULL_HANDLE;
        }
    }
    public abstract void build(VKContext ctxt);

    public void destroy(VKContext vkContext) {
        destroyRenderPass(vkContext);
        attachments.free();
    }

    public long get() {
        return renderPass;
    }

    public void initClearValues(boolean inverseZ) {
        if (this.hasDepthAttachement) {
            definedClearValueDepth.set(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f, 0);
        }
        int maxAttWithClear = 0;
        for (int i = 0; i < this.nAttachments; i++) {
            VkAttachmentDescription n = this.attachments.get(i);
            if (n.loadOp() == VK_ATTACHMENT_LOAD_OP_CLEAR) {
                maxAttWithClear = i+1;
            }
        }
        renderPassClearValues.clear();
        for (int i = 0; i < maxAttWithClear; i++) {
            renderPassClearValues.put(this.definedClearValues.get(i));
        }
        renderPassClearValues.flip();
    }
    public VkClearDepthStencilValue getClearValueDepth() {
        return definedClearValueDepth;
    }

    void buildRenderPass(VKContext ctxt, Buffer subpasses, org.lwjgl.vulkan.VkSubpassDependency.Buffer subpassDependencies) {
        try ( MemoryStack stack = stackPush() ) 
        {
            attachments.limit(nAttachments);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pNext(NULL)
                    .pAttachments(attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(null);
    
            LongBuffer pRenderPass = stack.longs(0);
            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
            this.renderPass = pRenderPass.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create render pass: " + VulkanErr.toString(err));
            }
        }
    }

    public VkAttachmentDescription getAttachmentDesc(int i) {
        return this.attachments.get(i);
    }
    
    @Override
    public String toString() {
        return this.name+"["+nAttachments+" attachments, hasdepth="+hasDepthAttachement+", clearValueCount="+renderPassClearValues.remaining()+"]";
    }
}
