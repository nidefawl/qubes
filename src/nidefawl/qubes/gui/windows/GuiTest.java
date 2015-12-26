package nidefawl.qubes.gui.windows;

public class GuiTest extends GuiWindow {
    public GuiTest() {
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        super.renderButtons(fTime, mX, mY);
    }

    public String getTitle() {
        return "Test";
    }

}
