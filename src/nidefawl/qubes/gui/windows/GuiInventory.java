package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.shader.Shaders;

public class GuiInventory extends GuiInventoryBase {
    

    public GuiInventory() {
    }
    public String getTitle() {
        return "Inventory";
    }
    @Override
    public void initGui(boolean first) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            close();
            return;
        }
        this.slots = p.getSlots(0);
        System.out.println("bounds "+this.bounds);;
        if (this.bounds != null) {
            setPos(this.bounds[0], this.bounds[1]);
            setSize(this.bounds[2], this.bounds[3]);
        } else {
            int rows = this.slots.getSlots().size()/10;
            int width = 20 + (slotBDist+slotW)*10;
            int height = titleBarHeight + 20+ (slotBDist+slotW)*rows;
            int xPos = (Game.displayWidth-width)/2;
            int yPos = (Game.displayHeight-height)/2;
            setPos(xPos, yPos);
            setSize(width, height);
        }
        
    }

    public void update() {
    }

    public void render(float fTime, double mX, double mY) {
        Shaders.colored.enable();
        renderSlots(fTime, mX, mY);
        super.renderButtons(fTime, mX, mY);
    }

}