package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.input.Keybinding;
import nidefawl.qubes.input.Keyboard;

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
            Keybinding key = KeybindManager.getKeyBindingByName("show_select_world");
            String name = Keyboard.getKeyName(key.getKey());
            String desc = " ("+name+")";
            Button controls = new Button(2, "Worlds"+desc);
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            Keybinding key = KeybindManager.getKeyBindingByName("show_look");
            String name = Keyboard.getKeyName(key.getKey());
            String desc = " ("+name+")";
            Button controls = new Button(4, "Adjust Character"+desc);
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            Keybinding key = KeybindManager.getKeyBindingByName("show_inventory");
            String name = Keyboard.getKeyName(key.getKey());
            String desc = " ("+name+")";
            Button controls = new Button(5, "Inventory"+desc);
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            Keybinding key = KeybindManager.getKeyBindingByName("show_crafting");
            String name = Keyboard.getKeyName(key.getKey());
            String desc = " ("+name+")";
            Button controls = new Button(6, "Crafting"+desc);
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            settings = new Button(7, "Settings");
            this.add(settings);
            settings.setPos(left, this.posY+this.height/2+20);
            settings.setSize(w1, h);
        }
        {
            controls = new Button(8, "Controls");
            this.add(controls);
            controls.setPos(left, this.posY+this.height/2+60);
            controls.setSize(w1, h);
        }
        {
            back = new Button(9, "Disconnect");
            this.add(back);
            back.setPos(left, this.posY+this.height/2+100);
            back.setSize(w1, h);
        }
        int top = this.posY+(this.height/2)-(this.buttons.size()*(h+30))/3;
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
        if (element.id == 4) {
            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_look").fire();
            return true;
        }
        if (element.id == 5) {
            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_inventory").fire();
            return true;
        }
        if (element.id == 6) {
            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_crafting").fire();
            return true;
        }
        if (element.id == 2) {
            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_select_world").fire();
            return true;
        }
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
