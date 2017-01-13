package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.gui.windows.GuiContext;

public class GuiMultiplayer extends Gui implements ITextEdit {

    final public FontRenderer font;
    private TextField field;
    private Button connect;
    private Button back;
    public GuiMultiplayer(GuiMainMenu parent) {
        this.isFullscreen=true;
        this.parent = parent;
        this.font = parent.font;
    }
    
    @Override
    public void initGui(boolean first) {
        this.clearElements();
        int w1 = 300;
        int h = 30;
        int left = this.posX+this.width/2-w1/2;
        {
            connect = new Button(1, "Connect");
            this.add(connect);
            connect.setPos(left+200, this.posY+this.height/2-20);
            connect.setSize(100, h);
        }
        {

            this.field = new TextField(this, 2, Game.instance.serverAddr);
            field.setPos(left, this.posY+this.height/2-20);
            field.setSize(w1-110, h);
//            field.
            this.add(field);
        }
        int offset = 40;
        offset+=20;
        {
            back = new Button(4, "Back");
            this.add(back);
            back.setPos(left, this.posY+this.height/2+offset);
            back.setSize(w1, h);
            offset+=40;
        }
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element == this.connect) {
            connect();
        }
        if (element == this.back) {
            Game.instance.showGUI((Gui) parent);
        }
        return true;
    }
    @Override
    public void submit(TextInput textInputRenderer) {
        connect();
    }
    /**
     * 
     */
    private void connect() {
        String s = field.getText();
        if (!s.isEmpty()) {
            Game.instance.connectTo(s);
            Game.instance.settings.lastserver = s;
            Game.instance.saveSettings();
        }
    }
    @Override
    public void onEscape(TextInput textInput) {
        this.field.focused = false;
        if (GuiContext.input == this.field) {
            GuiContext.input=null;
        }
    }

    public boolean requiresTextInput() {
        return true;
    }

}
