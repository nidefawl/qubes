package nidefawl.qubes.gui;

import nidefawl.qubes.shader.Shaders;

public class GuiBG extends AbstractUI {

    @Override
    public void render(float fTime, double mX, double mY) {
        Shaders.gui.enable();
        Shaders.gui.setProgramUniform1f("fade", 0.1f);
        renderBox(false, true, color2, color3);
        Shaders.gui.setProgramUniform1f("fade", 0.3f);
    }

    @Override
    public void initGui(boolean first) {
    }

}
