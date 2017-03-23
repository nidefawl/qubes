/**
 * 
 */
package nidefawl.qubes.item;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vulkan.VkDescLayouts;
import nidefawl.qubes.vulkan.VkDescriptor;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemRenderer {

    public FontRenderer font;
    private VkDescriptor descTextureItem;

    public ItemRenderer() {
    }

    /**
     * 
     */
    public void init() {
        this.font = FontRenderer.get(0, 16, 1);
        if (Engine.isVulkan) {
            this.descTextureItem = Engine.vkContext.descLayouts.allocDescSetSampleSingle();
        }
    }

    public void drawItem(BaseStack stack, float x, float y, float w, float h) {
        if (stack.isItem()) {
            ItemStack itemStack = (ItemStack) stack;
            int tex = itemStack.getItemTexture();
            if (Engine.isVulkan) {
                Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureItem);
            } else {
                GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getItems());
            }
            Engine.setPipeStateItem();
            ITess tess = Engine.getTess();
            tess.setColorF(-1, 1);
            tess.setUIntLSB(tex);
            tess.add(x+w, y+0, 0, 1, 1);
            tess.add(x+0, y+0, 0, 0, 1);
            tess.add(x+0, y+h, 0, 0, 0);
            tess.add(x+w, y+h, 0, 1, 0);
            tess.drawQuads();
        } else {
            BlockStack blockStack = (BlockStack) stack;
//          float scale = w/32f;
            float scale = w/45f;
            Engine.blockDraw.setOffset(x+w/2.0f, y+h/2.0f, 0);
            Engine.blockDraw.setScale(scale);
            Engine.blockDraw.setRotation(18, 90+45, 0);
            Engine.blockDraw.drawBlockDefault(blockStack.getBlock(), blockStack.data, blockStack.getStackdata());
        }
    }

    /**
     * @param stack
     * @param f
     * @param g
     * @param h
     * @param i
     */
    public void drawItemOverlay(BaseStack stack, float x, float y, float w, float h) {
        int stackSize = 0;
        if (stack.isItem()) {
            stackSize = ((ItemStack)stack).size;
        } else {
            stackSize = ((BlockStack)stack).size;
        }
        if (stackSize != 0)  {
            float w2 = this.font.getStringWidth(""+stackSize);
            this.font.drawString(""+stackSize, x+w-w2-1, y+h+2, 0xf0f0f0, true, 1.0f);
        }
    }

    public void onResourceReload() {
        if (Engine.isVulkan) {

            this.descTextureItem.setBindingCombinedImageSampler(0, 
                    TextureArrays.itemTextureArrayVK.getView(), 
                    TextureArrays.itemTextureArrayVK.getSampler(), 
                    TextureArrays.itemTextureArrayVK.getImageLayout());
            this.descTextureItem.update(Engine.vkContext);
        }
    }

}
