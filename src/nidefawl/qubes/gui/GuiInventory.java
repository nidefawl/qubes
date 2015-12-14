package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.inventory.InventorySlots;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.network.packet.PacketCSwitchWorld;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameMath;

public class GuiInventory extends Gui {

    final public FontRenderer font;
    private InventorySlots slots;
    int slotW = 48;
    int slotBDist = 2;

    public GuiInventory() {
        this.font = FontRenderer.get(null, 22, 0, 26);
        this.slots = new InventorySlots(Game.instance.getPlayer().getInventory(), 0, 0, slotW, slotBDist);
    }
    @Override
    public void initGui(boolean first) {
        int rows = this.slots.getSlots().size()/10;
        int width = 20 + (slotBDist+slotW)*10;
        int height = 20 + (slotBDist+slotW)*rows + 40 + 30;
        int xPos = (Game.displayWidth-width)/2;
        int yPos = (Game.displayHeight-height)/2;
        setPos(xPos, yPos);
        setSize(width, height);
        System.out.println("inv");
        this.buttons.clear();
        {
            this.buttons.add(new Button(1, "Back"));
            int btnW  = 120;
            int btnH = 24;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - btnW / 2, yPos+height-38);
            this.buttons.get(0).setSize(btnW, btnH);
        }
    }

    public void update() {
    }


    public void render(float fTime, double mX, double mY) {
        
        Shaders.colored.enable();
        int e = this.extendx;
        float r = this.round;
        this.round = 1;
        this.extendx = 0;
        Shaders.gui.enable();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        this.round = 2;
        renderRoundedBoxShadow(posX, posY, -10, width, height, color, 1, true);
        float  f = this.shadowSigma;
        this.shadowSigma = 0.4f;
        renderRoundedBoxShadow(posX-1, posY, 0, width+2, 32, -1, 0.8f, true);
        this.round = 5;
        renderSlots(fTime, mX, mY);
        this.shadowSigma = f;
        this.round = r;
        this.extendx = e;
        Shaders.textured.enable();
        font.drawString("Inventory", posX+10, posY+27, -1, true, 1f);

        super.renderButtons(fTime, mX, mY);

    }

    private void renderSlots(float fTime, double mX, double mY) {
        float posx = this.posX+10;
        float posy  = this.posY+40;
        Shaders.gui.enable();
        Slot sHover = null;
        float inset = 4;
        float inset2 = 2;
        for (Slot s : this.slots.getSlots()) {
            if (s.isAt(mX-posx, mY-posy)) {
                sHover = s;
            }
            renderSlotBackground(posx+s.x, posy+s.y, 1, s.w, s.w, -1, 0.8f, true);
        }
        if (sHover != null) {
            this.round = 3;
            renderSlotBackground(posx+sHover.x+inset2, posy+sHover.y+inset2, 32, sHover.w-inset2*2, sHover.w-inset2*2, -1, 0.6f, false);
            this.round = 5;
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (Slot s : this.slots.getSlots()) {
            BaseStack stack = s.getItem();
            if (stack != null) {
                Engine.itemRender.drawItem(stack, posx+s.x+inset, posy+s.y+inset, s.w-inset*2, s.w-inset*2);
            }
        }
        Shaders.textured.enable();
        for (Slot s : this.slots.getSlots()) {
            BaseStack stack = s.getItem();
            if (stack != null) {
                Engine.itemRender.drawItemOverlay(stack, posx+s.x+inset, posy+s.y+inset, s.w-inset*2, s.w-inset*2);
            }
        }
        Shaders.gui.enable();
        if (sHover != null) {
            this.round = 3;
            renderSlotBackground(posx+sHover.x+inset2, posy+sHover.y+inset2, 32, sHover.w-inset2*2, sHover.w-inset2*2, -1, 0.2f, false);
            this.round = 5;
        }
        BaseStack stack = this.slots.getCarried();
        if (stack != null) {
            Engine.itemRender.drawItem(stack, (float)mX+inset-slotW/2, (float)mY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
            Shaders.textured.enable();
            Engine.itemRender.drawItemOverlay(stack, (float)mX+inset-slotW/2, (float)mY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
        }
        
        Shader.disable();

    }

    private void renderSlotBackground(float x, float y, float z, float w, float h, int color, float alpha, boolean shadow) {
        float ff = shadowSigma;
        shadowSigma = 2;
        int ex = extendx;
        int ey = extendy;
        extendx = 1;
        extendy = 1;
        renderRoundedBoxShadowInverse(x, y, z, w, h, color, alpha, shadow);
        shadowSigma = ff;
        extendx = ex;
        extendy = ey;

    }

    public boolean onMouseClick(int button, int action) {
        if (super.onMouseClick(button, action))
            return true;
        Slot s = this.slots.getSlotAt(Mouse.getX()-this.posX-10, Mouse.getY()-this.posY-40);
        if (s != null)
        this.slots.slotClicked(s, button, action);
        return s!=null;
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            Game.instance.showGUI(null);
        }
        return true;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (super.onKeyPress(key, scancode, action, mods)) {
            return true;
        }
        return true;
    }
}
