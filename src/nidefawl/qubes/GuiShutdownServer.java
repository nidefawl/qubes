package nidefawl.qubes;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.GuiMainMenu;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.shader.Shaders;

public class GuiShutdownServer extends Gui {

    final public FontRenderer font;

    public GuiShutdownServer() {
        this.font = FontRenderer.get(0, 18, 0);
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        Shaders.textured.enable();
        font.drawString("Saving game...", this.width / 2, this.height / 2 - 20, -1, true, 1, 2);
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

    }

    @Override
    public void update() {
        super.update();
        if (Game.instance.server.isShutdownDone()) {
            Game.instance.showGUI(null);
        }
    }

}
