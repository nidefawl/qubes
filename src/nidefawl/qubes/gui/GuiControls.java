package nidefawl.qubes.gui;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.input.Keybinding;
import nidefawl.qubes.input.Keyboard;
import nidefawl.qubes.shader.Shaders;

public class GuiControls extends Gui {

    static class Control extends AbstractUI {
        private Keybinding b;

        /**
         * @param string
         * @param g 
         * @param string2
         */
        public Control(Keybinding b) {
            this.b = b;
        }

        @Override
        public void render(float fTime, double mX, double mY) {
            FontRenderer fr = ((ScrollList)parent).font;
            Shaders.colored.enable();
            this.hovered = this.mouseOver(mX, mY);
            int x = this.posX;
            int y = this.posY;
            int w = this.width;
            int h = this.height;
            int rw = 150;
            int r = (parent.width)-rw-15;
            this.posX = r;
            this.width = rw;
            AbstractUI g = selectedButton;
            if (this.hovered)
            selectedButton = this;
            posY+=2;
            height-=4;
            renderBox();
            selectedButton = g;
            this.width = w;
            this.posX = x;
            this.posY = y;
            this.height = h;
            int c1 = this.hovered ? -1 : this.color;
            int c2 = this.hovered ? -1 : this.color2;
//            final Tess tessellator = Tess.instance;
            Shaders.textured.enable();
            fr.drawString(b.getName(), this.posX, this.posY+fr.centerY(height), -1, true, 0.9f);
////            GLfw.g
            String keyname = "";
            int c = -1;
            if (b.isEnabled() && b.getKey() != -1) {
                keyname = Keyboard.getKeyName(b.getKey());
                for (Keybinding b2 : KeybindManager.getBindings()) {
                    if (b2 != b && b.getKey() == b2.getKey()) {
                        c = 0xff6666;
                    }
                }
            }
            
            fr.drawString(keyname, r+rw/2, this.posY+fr.centerY(height), c, true, 0.9f, 2);
            Shaders.colored.enable();
            Tess tessellator = Tess.instance;
//          OpenGlHelper.glColor3f(fa, fa, fa);
          GL11.glLineWidth(1.0F);
          
//          GL11.glBegin(GL11.GL_LINE_STRIP);
          tessellator.setColorF(-1, 0.1f);

          tessellator.add(this.posX + w - 12, this.posY + h + 3);
          tessellator.setColorF(-1, 0.4f);
          tessellator.add(this.posX + 2 +(w-12)/2, this.posY + h + 3);
          tessellator.setColorF(-1, 0.1f);

          tessellator.add(this.posX + 2, this.posY + h + 3);
          tessellator.draw(GL11.GL_LINE_STRIP);
        }

        @Override
        public void initGui(boolean first) {
        }
    }
    Control selected = null;

    private Button            back;
    List<Control>             list = Lists.newArrayList();
    ScrollList scrolllist;

    private Button            clear;
    private Button            cancel;
    private Button            defaultSet;
    public GuiControls(Gui parent) {
        this.parent = parent;
    }

    @Override
    public void initGui(boolean first) {
        scrolllist = new ScrollList(this);
        this.clearElements();
        this.list.clear();
        this.width = 630;
        this.posX = (Game.guiWidth-this.width)/2;
        this.posY = Game.guiHeight / 4;
        int scrollListHeight = 500;
        int w1 = this.width;
        int idx = 10;
        for (Keybinding b : KeybindManager.getBindings()) {
            if (b.isStaticBinding()){
                continue;
            }
            Control c = new Control(b);
            c.id = idx++;
            list.add(c);
        }
        int y = 0;
        for (Control s : list) {
            s.setPos(0, y);
            s.setSize(w1, 30);
            y += 36;
            scrolllist.add(s);
        }
        this.scrolllist.setSize(width-50, scrollListHeight);
        this.height = scrollListHeight+titleBarOffset+50;
        {
            clear = new Button(3, "None");
            clear.setPos(this.width / 2 - 160/2, this.height / 2 + 220);
            clear.setSize(160, 30);
            this.add(clear);
            clear.draw = false;
        }
        {
            cancel = new Button(4, "Cancel");
            cancel.setPos(this.width / 2 - 160/2, this.height / 2 + 220);
            cancel.setSize(160, 30);
            this.add(cancel);
            cancel.draw = false;
        }
        {
            defaultSet = new Button(5, "Set default");
            defaultSet.setPos(this.width / 2 - 160/2, this.height / 2 + 220);
            defaultSet.setSize(160, 30);
            this.add(defaultSet);
            defaultSet.draw = false;
        }
        {
            back = new Button(6, "Back");
            this.add(back);
            back.setSize(160, 30);
            back.setPos(this.width / 2 - 160/2, scrollListHeight+titleBarOffset+15);
        }
        this.posY = Game.guiHeight/2-this.height/2;
        this.scrolllist.setPos(posX+25, posY+titleBarOffset);
        this.add(this.scrolllist.scrollbarbutton);
    }

    public boolean onMouseClick(int button, int action) {
        if (selected != null) {
            return super.onMouseClick(button, action);
        }
        return super.onMouseClick(button, action) || this.scrolllist.onMouseClick(button, action);
    }

    public void update() {
        super.update();
        this.scrolllist.update();
    }
    
    Keybinding inUseKey = null;
    protected String getTitle() {
        return "Controls";
    }
    public void render(float fTime, double mX, double mY) {
        double mmX = mX;
        double mmY = mY;
        
        if (selected != null) {
            mmX = -9999;
            mmY = -9999;
        }
        renderBackground(fTime, mmX, mmY, true, 0.7f);
        Shaders.textured.enable();
//        this.font.drawString("Controls", this.posX+this.width / 2.0f, this.posY+5, -1, true, 1.0f, 2);
        this.scrolllist.render(fTime, mmX, mmY);
        this.clear.draw = this.selected != null;
        this.defaultSet.draw = this.selected != null;
        this.cancel.draw = this.selected != null;
        this.back.enabled = this.selected == null;
        if (selected != null) {
            String name = Keyboard.getKeyName(selected.b.getKey());
            int w = 300;
            int h = 150;
            int x = 0+width/2-w/2;
            int y = 0+height/2-h/2;
            int h2=h;
            if (inUseKey != null) {
                h2+=70;
            }
            int buttonW = 150;
            this.cancel.width = w-20;
            this.cancel.height = 20;
            this.cancel.posX = x+10;
            this.cancel.posY = y+h2-this.cancel.height-10;
            this.defaultSet.width = w/2-20;
            this.defaultSet.height = 24;
            this.defaultSet.posX = x+10;
            this.defaultSet.posY = this.cancel.posY-30;
            this.clear.width = w/2-20;
            this.clear.height = 24;
            this.clear.posX = x+w/2+10;
            this.clear.posY = this.cancel.posY-30;
            x += posX;
            y += posY;
            renderRoundedBoxShadow(x, y, 2, w, h2, color2, 0.7f, true);
            Shaders.textured.enable();
            this.font.drawString("Please press a key for", x+w/2, y+h/4, 0xf1f1f1, true, 1.0f, 2);
            this.font.drawString(selected.b.getName(), x+w/2, y+h/4*2-6, 0xf1ffff, true, 1.0f, 2);
            if (inUseKey != null) {
                String inUsename = Keyboard.getKeyName(inUseKey.getKey());
                this.font.drawString("Key "+inUsename+" is already in use:", x+w/2, y+h/4*3, 0xf19999, true, 1.0f, 2);
                this.font.drawString(inUseKey.getName(), x+w/2, y+h-10, 0xf19999, true, 1.0f, 2);
            } else {

//                this.font.drawString("ESC to cancel", x+w/2, y+h/4*3+15, -1, true, 1.0f, 2);
            }
            
        }
        super.renderButtons(fTime, mX, mY);
    }
    @Override
    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            
            if (this.selected != null) {
                if (key == GLFW.GLFW_KEY_ESCAPE) {
//                    if (inUseKey == null)
//                    this.selected.b.setEnabled(false);
                    this.selected = null;
                    this.inUseKey = null;
                } else {
                    Keybinding k = KeybindManager.getKeyBinding(key);
                    if (k != null && k != this.selected.b) {
                        inUseKey = k;
                        return true;
                    }
                    this.selected.b.setEnabled(true);
                    this.selected.b.setKey(key);
                    KeybindManager.updateKeybindMap();
                    KeybindManager.saveBindings();
                    this.selected = null;
                    
                }
                return true;
            }   
        }
        return super.onKeyPress(key, scancode, action, mods);
    }
    
    

    public boolean onGuiClicked(AbstractUI element) {
        if (element == clear) {
            if (this.selected != null) {
                this.selected.b.setKey(-1);
                KeybindManager.updateKeybindMap();
                KeybindManager.saveBindings();
                this.selected = null;
                this.inUseKey = null;
            }
            return true;
        }
        if (element == defaultSet) {
            if (this.selected != null) {
                this.selected.b.setKey(this.selected.b.getDefaultkey());
                KeybindManager.updateKeybindMap();
                KeybindManager.saveBindings();
                this.selected = null;
                this.inUseKey = null;
            }
            return true;
        }
        if (element == cancel) {
            this.selected = null;
            this.inUseKey = null;
            return true;
        }
        if (selected != null) {
            return true;
        }
        if (element == back) {
            Game.instance.showGUI((Gui) parent);
        }
        if (element instanceof Control) {
            Control ctrl = (Control) element;
            selected = ctrl;
//            Keyboard.getKeyName(ctrl.b.getKey());
//            ctrl
        }
        return true;
    }
    public boolean onWheelScroll(double xoffset, double yoffset) {
        if (selected != null) {
            return true;
        }
        this.scrolllist.onWheelScroll(xoffset, yoffset);
        return true;
    }

}
