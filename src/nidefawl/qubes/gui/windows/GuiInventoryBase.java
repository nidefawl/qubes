package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.Tooltip;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.packet.PacketCInvClick;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public abstract class GuiInventoryBase extends GuiWindow {


    protected Slots slots;

    protected void renderSlots(float fTime, double mX, double mY) {
        float posx = this.posX+10;
        float posy  = this.posY+titleBarHeight+5;
        Shaders.gui.enable();
        Slot sHover = null;
        float inset = 4;
        float inset2 = 3;
        Engine.pxStack.push();
        Engine.pxStack.translate(0, 0, 5);
        
        for (Slot s : this.slots.getSlots()) {
            if (s.isAt(mX-posx, mY-posy)) {
                sHover = s;
            }
            renderSlotBackground(posx+s.x, posy+s.y, 0, s.w, s.w, 0xdadada, 0.8f, true, 4);
//            renderSlotBackground(posx+s.x, posy+s.y, 1, s.w, s.w, 0xdadada, 0.8f, true, 1);
        }
        Engine.pxStack.translate(0, 0, 5);
        if (sHover != null) {
            renderSlotBackground(posx+sHover.x+inset2, posy+sHover.y+inset2, 4, sHover.w-inset2*2, sHover.w-inset2*2, -1, 0.6f, true, 1);
        }
        Engine.pxStack.translate(0, 0, 5);
        Shaders.textured.enable();
        for (Slot s : this.slots.getSlots()) {
            BaseStack stack = s.getItem();
            if (stack != null) {
                Engine.itemRender.drawItem(stack, posx+s.x+inset, posy+s.y+inset, s.w-inset*2, s.w-inset*2);
            }
        }
        Engine.pxStack.translate(0, 0, 5);
        for (Slot s : this.slots.getSlots()) {
            BaseStack stack = s.getItem();
            if (stack != null) {
                Shaders.textured.enable();
                Engine.itemRender.drawItemOverlay(stack, posx+s.x+inset, posy+s.y+inset+0, s.w-inset*2, s.w-inset*2);
            }
        }
        Engine.pxStack.translate(0, 0, 5);
        Shaders.gui.enable();
        if (sHover != null) {
            renderSlotBackground(posx+sHover.x+inset2, posy+sHover.y+inset2, 32, +sHover.w-inset2*2, sHover.w-inset2*2, -1, 0.16f, false, 2);
        }
        Engine.pxStack.pop();
        BaseStack stack = sHover != null ? sHover.getItem() : null;
        if (stack != null) {
            Tooltip tip = Tooltip.item.set(stack, sHover, this);
            tip.setPos((int)(posx+sHover.x+sHover.w+4), (int)(posy+sHover.y+sHover.w/2));
            GuiWindowManager.setTooltip(tip);
        }
        Shader.disable();

    }
    public boolean onMouseClick(int button, int action) {
        if (super.onMouseClick(button, action))
            return true;
        float posx = this.posX+10;
        float posy  = this.posY+titleBarHeight+5;
        Slot s = this.slots.getSlotAt(Mouse.getX()-posx, Mouse.getY()-posy);
        if (s != null) {
            BaseStack stack = this.slots.slotClicked(s, button, action);
            Game.instance.sendPacket(new PacketCInvClick(slots.getId(), s.idx, button, action, stack));
        }
        return s!=null;
    }
    protected boolean canResize() {
        return false;
    }
}
