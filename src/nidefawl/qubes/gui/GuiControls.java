package nidefawl.qubes.gui;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.input.InputController;
import nidefawl.qubes.input.Keybinding;
import nidefawl.qubes.input.Keyboard;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.ShaderBuffer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;

public class GuiControls extends Gui {

    static class Control extends AbstractUI {
        private Keybinding b;

        /**
         * @param string
         * @param g 
         * @param string2
         */
        public Control(Gui g, Keybinding b) {
            this.b = b;
            this.parent = g;
        }

        @Override
        public void render(float fTime, double mX, double mY) {
            FontRenderer fr = ((ScrollList)parent).font;
            Shaders.colored.enable();
            this.hovered = this.mouseOver(mX, mY);
            int x = this.posX;
            int w = this.width;
            int rw = 150;
//            System.out.println(parent.width+"/"+this.width);
//            System.out.println(parent);
            int r = (parent.width)-rw-30;
            this.posX = r;
            this.width = rw;
            Shaders.gui.enable();
            AbstractUI g = selectedButton;
            if (this.hovered)
            selectedButton = this;
            renderBox();
            selectedButton = g;
            this.width = w;
            this.posX = x;
            int c1 = this.hovered ? -1 : this.color;
            int c2 = this.hovered ? -1 : this.color2;
//            final Tess tessellator = Tess.instance;
            Shaders.textured.enable();
            fr.drawString(b.getName(), this.posX, this.posY+(this.height-5), -1, true, 0.9f);
////            GLfw.g
            String keyname = "";
            if (b.isEnabled()) {
                keyname = Keyboard.getKeyName(b.getKey());
            }
            fr.drawString(keyname, r+rw/2, this.posY+(this.height-5), -1, true, 0.9f, 2);
        }

        @Override
        public void initGui(boolean first) {
        }
    }
    Control selected = null;

    final public FontRenderer font;
    private Button            back;
    List<Control>             list = Lists.newArrayList();
    ScrollList scrolllist;

    public GuiControls(Gui parent) {
        this.font = FontRenderer.get(null, 18, 0, 20);
        this.parent = parent;
    }

    @Override
    public void initGui(boolean first) {
        scrolllist = new ScrollList(this);
        this.buttons.clear();
        this.list.clear();
        int w1 = 520;
        int h = 30;
        for (Keybinding b : InputController.getBindings()) {
            if (!b.isStaticBinding())
            list.add(new Control(scrolllist, b));
        }
        int left = this.posX + this.width / 2 - (w1/2);
        this.scrolllist.setPos(left, this.posY + this.height / 6 + 40);
        int y = 0;
        for (Control s : list) {
            s.setPos(0, y);
            s.setSize(w1, 30);
            y += 36;
            scrolllist.buttons.add(s);
        }
        this.scrolllist.setSize(w1, this.height / 2);
        {
            back = new Button(6, "Back");
            this.buttons.add(back);
            back.setPos(this.posX + this.width / 2 - 160/2, this.posY + this.height / 2 + 220);
            back.setSize(160, 30);
        }
        this.buttons.add(this.scrolllist.scrollbarbutton);
    }

    public boolean onMouseClick(int button, int action) {
        if (selected != null) {
            return true;
        }
        return super.onMouseClick(button, action) || this.scrolllist.onMouseClick(button, action);
    }

    public void update() {
        this.scrolllist.update();

    }
    Keybinding inUseKey = null;
    public void render(float fTime, double mX, double mY) {
        if (selected != null) {
            mX = -9999;
            mY = -9999;
        }
        renderBackground(fTime, mX, mY, true, 0.7f);
        Shaders.textured.enable();
        this.font.drawString("Controls", this.posX + this.width / 2.0f, this.posY + this.height / 6, -1, true, 1.0f, 2);
        this.scrolllist.render(fTime, mX, mY);
        super.renderButtons(fTime, mX, mY);
        if (selected != null) {
            String name = Keyboard.getKeyName(selected.b.getKey());
            int w = 300;
            int h = 150;
            int x = this.posX+width/2-w/2;
            int y = this.posY+height/2-h/2;
            Shaders.gui.enable();
            renderRoundedBoxShadow(x, y, 2, w, h, color2, 0.7f, true);
            Shaders.textured.enable();
            this.font.drawString("Please press a key for", x+w/2, y+h/4, 0xf1f1f1, true, 1.0f, 2);
            this.font.drawString(selected.b.getName(), x+w/2, y+h/4*2-6, 0xf1ffff, true, 1.0f, 2);
            if (inUseKey != null) {
                String inUsename = Keyboard.getKeyName(inUseKey.getKey());
                this.font.drawString("Key "+inUsename+" is already in use:", x+w/2, y+h/4*3, 0xf19999, true, 1.0f, 2);
                this.font.drawString(inUseKey.getName(), x+w/2, y+h-10, 0xf19999, true, 1.0f, 2);
            } else {

                this.font.drawString("ESC to cancel", x+w/2, y+h/4*3+15, -1, true, 1.0f, 2);
            }
            
        }
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
                    Keybinding k = InputController.getKeyBinding(key);
                    if (k != null && k != this.selected.b) {
                        inUseKey = k;
                        return true;
                    }
                    this.selected.b.setEnabled(true);
                    this.selected.b.setKey(key);
                    InputController.saveBindings();
                    InputController.updateKeybindMap();
                    this.selected = null;
                    
                }
                return true;
            }   
        }
        return super.onKeyPress(key, scancode, action, mods);
    }
    
    

    public boolean onGuiClicked(AbstractUI element) {
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
    public void onWheelScroll(double xoffset, double yoffset) {
        if (selected != null) {
            return;
        }
        this.scrolllist.onWheelScroll(xoffset, yoffset);
    }

}
