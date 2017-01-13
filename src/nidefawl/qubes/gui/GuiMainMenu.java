package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.network.client.ClientHandler;
import nidefawl.qubes.shader.Shaders;

public class GuiMainMenu extends Gui implements ITextEdit {

    private TextField fieldN;
    private Button singleplayer;
    private Button multiplayer;
    private Button settings;
    private Button controls;
    private Button quit;
    
    public GuiMainMenu() {
        this.isFullscreen=true;
    }
    @Override
    public void initGui(boolean first) {
        Game.instance.setConnection(null);
        Game.instance.server.stop();
        this.clearElements();
        int w1 = 300;
        int h = 30;
        int left = this.posX+this.width/2-w1/2;
        {
            String nName = "Name";

            FontRenderer f = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
            int a = (int) (f.getStringWidth(nName)+12);
            this.fieldN = new TextField(this, 10, Game.instance.getProfile().getName());
            fieldN.setPos(left+a, this.posY+this.height/2-60);
            fieldN.setSize(w1-a, h);
            this.add(fieldN);
        }
        int offset = 20;
        {
            singleplayer = new Button(1, "Singleplayer");
            this.add(singleplayer);
            singleplayer.setPos(left, this.posY+this.height/2+offset);
            singleplayer.setSize(w1, h);
            offset+=40;
        }
        {
            
            multiplayer = new Button(3, "Multiplayer");
            this.add(multiplayer);
            multiplayer.setPos(left, this.posY+this.height/2+offset);
            multiplayer.setSize(w1, h);
            offset+=40;
        }
        {
            settings = new Button(3, "Settings");
            this.add(settings);
            settings.setPos(left, this.posY+this.height/2+offset);
            settings.setSize(w1, h);
            offset+=40;
        }
        {
            controls = new Button(4, "Controls");
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+offset);
            controls.setSize(w1, h);
            offset+=40;
        }
        offset+=20;
        {
            quit = new Button(4, "Quit Game");
            this.add(quit);
            quit.setPos(left, this.posY+this.height/2+offset);
            quit.setSize(w1, h);
            offset+=40;
        }
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        FontRenderer f = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
        int w1 = 300;
        int h = 30;
        int left = this.posX+this.width/2-w1/2;
        Shaders.textured.enable();
        f.drawString("Name", left, this.posY+this.height/2-35, -1, true, 1);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element == this.singleplayer) {
            startSinglePlayer();
        }
        if (element == this.multiplayer) {
            String s2 = fieldN.getText();
            if (!s2.isEmpty()) {
                Game.instance.getProfile().setName(s2);
                Game.instance.saveProfile();
            }
            Game.instance.showGUI(new GuiMultiplayer(this));
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
        startSinglePlayer();
    }
    /**
     * 
     */
    private void startSinglePlayer() {
        String s2 = fieldN.getText();
        if (!s2.isEmpty()) {
            Game.instance.getProfile().setName(s2);
            Game.instance.saveProfile();
            Game.instance.server.start();
            Game.instance.connectTo("localhost:21087");
        }
    }
    @Override
    public void onEscape(TextInput textInput) {
        this.fieldN.focused = false;
        if (GuiContext.input == this.fieldN) {
            GuiContext.input=null;
        }
    }

    public boolean requiresTextInput() {
        return true;
    }

}
