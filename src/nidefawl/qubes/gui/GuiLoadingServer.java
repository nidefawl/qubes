package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.shader.Shaders;

public class GuiLoadingServer extends Gui {


    public GuiLoadingServer() {
        isFullscreen = true;
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        Shaders.textured.enable();
        font.drawString("Loading game...", this.width / 2, this.height / 2 - 20, -1, true, 1, 2);
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

    }

    @Override
    public void update() {
        super.update();
        if (Game.instance.server.isReady()) {
            Game.instance.connectTo("localhost:21087");
        }
    }

}
