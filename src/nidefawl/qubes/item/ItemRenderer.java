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

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemRenderer {

    public FontRenderer font;

    public ItemRenderer() {
    }

    /**
     * 
     */
    public void init() {
        this.font = FontRenderer.get(null, 16, 1, 18);
    }

    public void drawItem(BaseStack stack, float x, float y, float w, float h) {
        if (stack.isItem()) {
            Shaders.item.enable();
            ItemStack itemStack = (ItemStack) stack;
            int tex = itemStack.getItemTexture();
            GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getItems());
            Tess.instance.setColorF(-1, 1);
            Tess.instance.setUIntLSB(tex);
            Tess.instance.add(x+w, y+0, 0, 1, 1);
            Tess.instance.add(x+0, y+0, 0, 0, 1);
            Tess.instance.add(x+0, y+h, 0, 0, 0);
            Tess.instance.add(x+w, y+h, 0, 1, 0);
            Tess.instance.draw(GL11.GL_QUADS);
        } else {
            Shaders.textured.enable();
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
            int w2 = this.font.getStringWidth(""+stackSize);
            this.font.drawString(""+stackSize, x+w-w2-1, y+h+2, 0xf0f0f0, true, 1.0f);
        }
    }


}
