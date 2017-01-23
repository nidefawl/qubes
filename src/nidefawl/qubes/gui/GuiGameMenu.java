package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import nidefawl.qubes.Game;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.input.Keybinding;
import nidefawl.qubes.input.Keyboard;
import nidefawl.qubes.shader.Shaders;

public class GuiGameMenu extends Gui {

    private Button resume;
    private Button settings;
    private Button controls;
    private Button back;
    private Button quit;

    public GuiGameMenu() {
    }
    public void renderBackground(float fTime, double mX, double mY, boolean b, float a) {
        if (Game.instance != null && Game.instance.getWorld() != null) {
            a = 0.9f;
        } else {
            a = 1.0f;
        }
        int c2 = this.hovered || this.focused ? this.color4 : this.color2;
        Shaders.gui.enable();
        if (this.resume != null&&this.back!=null)  {
            int brd = 0;
            shadowSigma=100;
            renderRoundedBoxShadow(this.posX-brd, this.posY-brd, 0, this.width+brd*2, this.height+brd*2, c2, this.alpha2, true);
            brd+=5;
            resetShape();

            renderRoundedBoxShadow(this.posX-brd, this.posY-brd, 0, this.width+brd*2, this.height+brd*2, c2, this.alpha2, false);
        }
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        int w1 = 240;
        int h = 30;
        int left = this.posX+width/2-w1/2;
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
        int brd = 32;
        this.posX = this.resume.posX-brd;
        this.posY = this.resume.posY-brd;
        this.width = this.resume.width+brd*2;
        this.height = (this.back.posY+this.back.height+brd)-(this.resume.posY-brd);
        for (AbstractUI b : this.buttons) {
            b.posX -= this.posX;
            b.posY -= this.posY;
        }
    }
    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 4) {
//            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_look").fire();
            return true;
        }
        if (element.id == 5) {
//            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_inventory").fire();
            return true;
        }
        if (element.id == 6) {
//            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_crafting").fire();
            return true;
        }
        if (element.id == 2) {
//            Game.instance.showGUI(null);
            KeybindManager.getKeyBindingByName("show_select_world").fire();
            return true;
        }
        if (element == this.resume) {
            GuiWindowManager.closeAll();
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
