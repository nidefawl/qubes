package nidefawl.qubes.gui;

import java.util.Arrays;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ColorPicker;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.gui.windows.GuiContext;

public class GuiMainMenu extends Gui implements ITextEdit {

    final public FontRenderer font;
    final FontRenderer fontSmall;
    private TextField field;
    private TextField fieldN;
    private Button connect;
    private Button settings;
    private Button controls;
    private Button quit;
    private Button crash;
    public GuiMainMenu() {
        this.font = FontRenderer.get(0, 18, 0);
        this.fontSmall = FontRenderer.get(0, 14, 0);
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
        {

            this.fieldN = new TextField(this, 10, Game.instance.getProfile().getName());
            fieldN.setPos(left, this.posY+this.height/2-60);
            fieldN.setSize(w1, h);
//            field.
            this.add(fieldN);
        }
        {
            settings = new Button(3, "Settings");
            this.add(settings);
            settings.setPos(left, this.posY+this.height/2+20);
            settings.setSize(w1, h);
        }
        {
            controls = new Button(4, "Controls");
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            quit = new Button(4, "Quit Game");
            this.add(quit);
            quit.setPos(left, this.posY+this.height/2+120);
            quit.setSize(w1, h);
        }
        {
            crash = new Button(5, "Crash");
            this.add(crash);
            crash.setPos(left, this.posY+this.height/2+160);
            crash.setSize(w1, h);
        }
    }
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action);
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
//        element.posX+=30;
//        if (element.posX+element.width>this.width) {
//            element.posX = 0;
//        }
        if (element == this.connect) {
            connect();
        }
        if (element == this.crash) {
            String s = null;
            int a = s.length();
        }
        if (element == this.settings) {
            Game.instance.showGUI(new GuiSettings(this));
        }
        if (element == this.controls) {
            Game.instance.showGUI(new GuiControls(this));
        }
        if (element == this.quit) {
            Game.instance.shutdown();
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
        String s2 = fieldN.getText();
        if (!s.isEmpty() && !s2.isEmpty()) {
            Game.instance.getProfile().setName(s2);
            Game.instance.saveProfile();
            Game.instance.connectTo(s);    
        }
    }
    @Override
    public void onEscape(TextInput textInput) {
        this.field.focused = false;
        this.fieldN.focused = false;
        if (GuiContext.input == this.field) {
            GuiContext.input=null;
        }
        if (GuiContext.input == this.fieldN) {
            GuiContext.input=null;
        }
    }

    public boolean requiresTextInput() {
        return true;
    }

}
