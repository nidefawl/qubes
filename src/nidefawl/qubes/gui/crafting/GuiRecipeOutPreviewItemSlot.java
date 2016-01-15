package nidefawl.qubes.gui.crafting;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.Tooltip;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.shader.Shaders;

class GuiRecipeOutPreviewItemSlot extends AbstractUI {

    private GuiCraftingProgressEntry ctrl;

    public GuiRecipeOutPreviewItemSlot(GuiCraftingProgressEntry guiCraftingProgressEntry) {
        this.parent = guiCraftingProgressEntry;
        this.ctrl=guiCraftingProgressEntry;
    }

    public void initGui(boolean first) {

    }

    @Override
    public void render(float fTime, double mX, double mY) {
        if (!this.draw)
            return;
        BaseStack stack = this.ctrl.getStack();
        this.hovered = this.mouseOver(mX, mY);
        int c2 = this.hovered ? this.color4 : this.color2;
        float inset = 4;
        float inset2 = 3;
        Engine.pxStack.push(0, 0, 1);
        Shaders.gui.enable();
        int color = 0xdadada;
        float alpha = 0.8f;
        renderSlotBackground(posX, posY, 0, width, width, color, alpha, true, 4);

        if (this.hovered) {
            Engine.pxStack.translate(0, 0, 2);
            Shaders.gui.enable();
            renderSlotBackground(posX + inset2, posY + inset2, 0, width - inset2 * 2, width - inset2 * 2, -1, 0.6f, true, 1);
        }
//        Engine.pxStack.translate(0, 0, 2);
//        if (((CraftingProgressEntry)parent).isSelected()) {
//            renderSlotBackground(posX + inset, posY + inset, 0, width - inset * 2, width - inset * 2, -1, 1f, false, 1);
//        }

        Engine.pxStack.translate(0, 0, 2);

        if (stack != null) {
            Shaders.textured.enable();
            Engine.itemRender.drawItem(stack, posX+inset, posY+inset, width-inset*2, width-inset*2);
            Engine.pxStack.translate(0, 0, 2);
            Shaders.textured.enable();
            int nMult = this.ctrl.getMgr().getAmount()*stack.getSize();
            if (nMult > 0) {
                Shaders.textured.enable();
                Engine.pxStack.translate(0, 0, 10);
                FontRenderer font = Engine.itemRender.font;
                float w2 = font.getStringWidth(""+nMult);
                font.drawString(""+nMult, posX+inset+width-inset*2-w2-1, posY+inset+width-inset*2+2, 0xf0f0f0, true, 1.0f);
            }
            if (this.hovered) {
                Tooltip tip = Tooltip.item.set(stack, null, null);
                float offsetY = 0;
                float mxoffset = (float) ((Gui)parent).mouseOffsetX();
                float myoffset = (float) ((Gui)parent).mouseOffsetY();
                if (GuiContext.scrolllist != null) {
                    offsetY+=GuiContext.scrolllist.scrollY;
                    if (GuiContext.scrolllist != parent) {
                        mxoffset = (float) GuiContext.scrolllist.mouseOffsetX();
                        myoffset = (float) GuiContext.scrolllist.mouseOffsetY();
//                         myoffset += (float) ((Gui)parent).mouseOffsetY();
                    }
                }
                tip.setPos((int) (mxoffset+posX + width-2), (int) (myoffset-offsetY + posY+10));
                GuiWindowManager.setTooltip(tip);
            }
        }
        if (this.hovered) {
            Engine.pxStack.translate(0, 0, 10);
            Shaders.gui.enable();
            renderSlotBackground(posX + inset2, posY + inset2, 32, width - inset2 * 2, width - inset2 * 2, -1, 0.36f, false, 2);

        }

        Engine.pxStack.pop();

    }

}