package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.network.packet.PacketCSwitchWorld;
import nidefawl.qubes.shader.Shaders;

public class GuiSelectWorld extends Gui {

    final public FontRenderer font;

    public GuiSelectWorld() {
        this.font = FontRenderer.get(null, 18, 0, 20);
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        {
            this.add(new Button(1, "Back"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height / 2 + 70);
            this.buttons.get(0).setSize(w, h);
        }
    }

    public void update() {
    }

    public void render(float fTime, double mX, double mY) {
        Shaders.colored.enable();
        Tess.instance.setColor(2, 128);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
        Tess.instance.setColor(2, 255);
        int x1 = this.width / 5*2;
        int x2 = this.width - x1;
        int y1 = this.height / 5*2;
        int y2 = this.height - y1;
        Tess.instance.add(x1, y2);
        Tess.instance.add(x2, y2);
        Tess.instance.add(x2, y1);
        Tess.instance.add(x1, y1);
        Tess.instance.draw(GL_QUADS);
        Shaders.textured.enable();
        font.drawString("Please press [1-9]", this.width / 2, this.height / 2 - 20, -1, true, 1, 2);
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

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
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int world = key - GLFW.GLFW_KEY_1;
            Game.instance.sendPacket(new PacketCSwitchWorld(world));
            Game.instance.showGUI(null);
        }
        return true;
    }
}
