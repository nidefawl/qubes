package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.Tooltip;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.inventory.slots.SlotInventory;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.packet.PacketCInvClick;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public abstract class GuiWindowInventoryBase extends GuiWindow {


    protected Slots slots;

    protected void renderSlots(float fTime, double mX, double mY) {
        float posx = this.posX+10;
        float posy  = this.posY+titleBarHeight+5;
        renderSlots(this.slots, fTime, mX, mY, posx, posy);
    }
    public boolean onMouseClick(int button, int action) {
        if (super.onMouseClick(button, action))
            return true;
        float posx = this.posX+10;
        float posy  = this.posY+titleBarHeight+5;
        if (this.slots != null) {
            Slot s = this.slots.getSlotAt(mouseGetX()-posx, mouseGetY()-posy);
            if (s != null) {
                BaseStack stack = this.slots.slotClicked(s, button, action);
                Game.instance.sendPacket(new PacketCInvClick(slots.getId(), s.idx, button, action, stack));
            }
            return s!=null;
        }
        return false;
    }
    protected boolean canResize() {
        return false;
    }
}
