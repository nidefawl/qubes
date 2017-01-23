package nidefawl.qubes.gui;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.inventory.slots.SlotInventory;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.item.*;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;

public abstract class Tooltip extends Gui {
    protected GuiWindow window;
    public static class ItemToolTip extends Tooltip {
        private BaseStack stack;
        private Slot slot;
        private String title;
        FontRenderer fr;
        FontRenderer fr2;

        public ItemToolTip set(BaseStack stack, Slot slot, GuiWindow window) {
            this.fr = FontRenderer.get(0, 16, 1);
            this.fr2 = FontRenderer.get(0, 14, 0);
            if (this.stack != stack) {
                this.width = height = 0;
                this.stack = stack;   
                String name;
                if (this.stack.isBlock()) {
                    name = ((BlockStack)this.stack).getBlock().getName();
                } else {
                    Item item = ((ItemStack)stack).getItem();
                    name = item.getName();
                }
                this.title = name;
                this.width = GameMath.round(Math.max(this.width, fr.getStringWidth(this.title)+16));
                this.height = GameMath.round(Math.max(this.height, fr.getLineHeight()+10));
            }
            this.slot = slot;
            this.window = window;
            return this;
        }
        @Override
        public void render(float fTime, double mX, double mY) {
            Shaders.gui.enable();
            resetShape();
            alpha2=1;
            Shaders.gui.setProgramUniform1f("fade", 0.3f);
            renderBox();
            Shaders.gui.setProgramUniform1f("fade", 0.3f);
            Shaders.textured.enable();
            int y = GameMath.round(this.posY+fr.centerY(this.height));
            fr.drawString(this.title, posX+8, y, -1, false, 1, 0);
            y+=fr.getLineHeight();
            resetShape();
        }

        @Override
        public void initGui(boolean first) {
        }
        
    }
    public static ItemToolTip item = new ItemToolTip() {
    };
    public GuiWindow getTooltipOwner() {
        return this.window;
    }
}
