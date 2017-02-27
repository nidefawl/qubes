package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.network.client.ThreadConnect;
import nidefawl.qubes.shader.Shaders;

public class GuiConnecting extends Gui {

    String                    stateStr = "Connecting...";
    private ThreadConnect     thread;
    public GuiConnecting() {
        this.isFullscreen=!Game.instance.canRenderGui3d();
    }

    public GuiConnecting(ThreadConnect connect) {
        this();
        this.thread = connect;
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        {
            this.add(new Button(1, "Cancel"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height / 2 + 70);
            this.buttons.get(0).setSize(w, h);
        }
    }

    public void update() {
        super.update();
        stateStr = this.thread.getState();
        if (this.thread.connected && this.thread.finished) {
            Game.instance.showGUI(null);
        }
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        Engine.setPipeStateFontrenderer();
        font.drawString(this.stateStr, this.width / 2, this.height / 2 - 20, -1, true, 1, 2);
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

    }

    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            this.thread.cancel();
            Game.instance.showGUI(new GuiMainMenu());
        }
        return true;
    }

}
