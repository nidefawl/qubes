package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.shader.Shaders;

public class GuiDisconnected extends Gui {

    private String reason;
    public GuiDisconnected() {
        this.isFullscreen=!Game.instance.canRenderGui3d();
    }

    public GuiDisconnected(String reason) {
        this();
        this.reason = reason;
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        {
            this.add(new Button(1, "Main menu"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height / 2 + 70);
            this.buttons.get(0).setSize(w, h);
        }
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        font.drawString(this.reason, this.width / 2, this.height / 2 - 20, -1, true, 1, 2);
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

    }

    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            Game.instance.showGUI(new GuiMainMenu());
        }
        return true;
    }

}
