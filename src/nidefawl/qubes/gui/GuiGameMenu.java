package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gui.controls.Button;

public class GuiGameMenu extends Gui {

    final public FontRenderer font;
    final FontRenderer fontSmall;
    private Button resume;
    private Button settings;
    private Button controls;
    private Button back;
    private Button quit;

    public GuiGameMenu() {
        this.font = FontRenderer.get(0, 18, 0);
        this.fontSmall = FontRenderer.get(0, 14, 0);
    }
    @Override
    public void initGui(boolean first) {
        this.clearElements();
        int w1 = 240;
        int h = 30;
        int left = this.posX+this.width/2-w1/2;
        {
            resume = new Button(1, "Resume");
            this.add(resume);
            resume.setPos(left, this.posY+this.height/2-20);
            resume.setSize(w1, h);
        }
        {
            settings = new Button(2, "Settings");
            this.add(settings);
            settings.setPos(left, this.posY+this.height/2+20);
            settings.setSize(w1, h);
        }
        {
            controls = new Button(3, "Controls");
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            back = new Button(4, "Disconnect");
            this.add(back);
            back.setPos(left, this.posY+this.height/2+100);
            back.setSize(w1, h);
        }
        int top = this.posY+(this.height/2)-(this.buttons.size()*(h+20))/4;
        for (AbstractUI b : this.buttons) {
            b.posY = top;
            top += 20+b.height;
        }
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element == this.resume) {
            Game.instance.showGUI(null);
        }
        if (element == this.settings) {
            Game.instance.showGUI(new GuiSettings(this));
        }
        if (element == this.controls) {
            Game.instance.showGUI(new GuiControls(this));
        }
        if (element == this.back) {
            Game.instance.returnToMenu();
        }
        if (element == this.quit) {
            Game.instance.shutdown();
        }
        return true;
    }

    public boolean requiresTextInput() {
        return true;
    }

}
