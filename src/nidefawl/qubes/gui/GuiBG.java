package nidefawl.qubes.gui;

import nidefawl.qubes.render.gui.BoxGUI;

public class GuiBG extends AbstractUI {

    @Override
    public void render(float fTime, double mX, double mY) {
        BoxGUI.setFade(0.1f);
        renderBox(false, true, color2, color3);
        BoxGUI.setFade(0.3f);
    }

    @Override
    public void initGui(boolean first) {
    }

}
