package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.network.packet.PacketCInvClick;
import nidefawl.qubes.network.packet.PacketCInvTransaction;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.shader.Shaders;

public abstract class GuiInventoryResult extends Gui {

    public static class GuiInventoryResult1 extends GuiInventoryResult {
        @Override
        public int getInventory() {
            return 1;
        }
    }
    public static class GuiInventoryResult2 extends GuiInventoryResult {
        @Override
        public int getInventory() {
            return 2;
        }
    }
    public static class GuiInventoryResult3 extends GuiInventoryResult {
        @Override
        public int getInventory() {
            return 3;
        }
    }
    public static class GuiInventoryResult4 extends GuiInventoryResult {
        @Override
        public int getInventory() {
            return 4;
        }
    }
    public static Class<?> getWindowClass(int id) {
        switch (id) {
            case 0:
                return GuiInventoryResult1.class;
            case 1:
                return GuiInventoryResult2.class;
            case 2:
                return GuiInventoryResult3.class;
            case 3:
                return GuiInventoryResult4.class;
        }
        return null;
    }

    private Slots slots;

    public GuiInventoryResult() {
    }
    public abstract int getInventory();
    public String getTitle() {
        return "Crafted Items";
    }
    @Override
    public void initGui(boolean first) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            close();
            return;
        }
        clearElements();
        this.slots = p.getSlots(getInventory());
//        if (this.bounds != null) {
//            setPos(this.bounds[0], this.bounds[1]);
//            setSize(this.bounds[2], this.bounds[3]);
//        } else {
        int x = (int) ((Gui)this.parent).mouseOffsetX();
        int y = (int) ((Gui)this.parent).mouseOffsetY();
        x = (int) (mouseGetX()-x);
        y = (int) (mouseGetY()-y);
            int rows = this.slots.getSlots().size()/4;
            int width = 20 + (slotBDist+slotW)*4;
            int height = 15+ (slotBDist+slotW)*rows+32;
            int xPos = (int) (x-width/2);
            int yPos = (int) (y-height);
            setPos(xPos, yPos);
            setSize(width, height);
//        }
            Button b = new Button(1, "Take");
            b.setSize(width-30, 30);
            b.setPos(15, height-35);
            add(b);
        
    }

    public void render(float fTime, double mX, double mY) {
        BoxGUI.setFade(0.1f);
        renderBox(false, true, color2, color3);
        BoxGUI.setFade(0.3f);
        float posx = this.posX+10;
        float posy  = this.posY+10;
        renderSlots(slots, fTime, mX, mY, posx, posy);
        super.renderButtons(fTime, mX, mY);
    }
    
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            Game.instance.sendPacket(new PacketCInvTransaction(slots.getId(), 1));
        }
        ((Gui) this.parent).setPopup(null);
        return true;
    }


    public boolean onMouseClick(int button, int action) {
        if (super.onMouseClick(button, action)) {
            return true;
        }
        float posx = (float) (10+mouseOffsetX());
        float posy  = (float) (10+mouseOffsetY());
        if (this.slots != null) {
            System.out.println(this.slots.getSlot(0).x+", "+(mouseGetX()-posx));
            Slot s = this.slots.getSlotAt(mouseGetX()-posx, mouseGetY()-posy);
            if (s != null) {
                BaseStack stack = this.slots.slotClicked(s, button, action);
                Game.instance.sendPacket(new PacketCInvClick(slots.getId(), s.idx, button, action, stack));
                return true;
            }
        }
        System.err.println("popup close");
        ((Gui) this.parent).setPopup(null);
        return false;
    }
}
