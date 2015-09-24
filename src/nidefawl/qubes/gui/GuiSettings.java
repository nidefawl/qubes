package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.*;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiSettings extends Gui {

    final public FontRenderer font;
    private ComboBox combo;
    private ComboBox combo2;
    private Button back;

    public GuiSettings() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
    }
    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        int w1 = 160;
        int h = 30;
        {
            int left = this.posX+this.width/2-w1/2;
            combo = new ComboBox(3, "Rate this", true);
            this.buttons.add(combo);
            combo.setPos(left, this.posY+this.height/6+40);
            combo.setSize(w1, h);
            combo.titleWidth=135;
            combo.setValue("Please pick");
        }
        {
            int left = this.posX+this.width/2-w1/2;
            combo2 = new ComboBox(4, "Lots of entries", true);
            this.buttons.add(combo2);
            combo2.setPos(left, this.posY+this.height/6+85);
            combo2.setSize(w1, h);
            combo2.titleWidth=135;
            combo2.setValue("0");
        }
        {
            int left = this.posX+this.width/2-w1/2;
            back = new Button(6, "Back");
            this.buttons.add(back);
            back.setPos(left, this.posY+this.height/2+120);
            back.setSize(w1, h);
        }
    }
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action);
    }

    public void update() {
        
    }

    public void render(float fTime, double mX, double mY) {
        this.font.drawString("Settings", this.posX+this.width/2.0f, this.posY+this.height/6, -1, true, 1.0f);
        Shaders.colored.enable();
        Tess.instance.setColor(2, 255);
        Tess.instance.add(this.posX, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
//        Shaders.textured.enable();
//        Shader.disable();
        super.renderButtons(fTime, mX, mY);
 
    }

    public boolean onGuiClicked(AbstractUI element) {
        if (element == combo) {
            if (combo.onClick(this)) {
                final String[] vals = new String[] { "Awesome!!!", "Well, pretty Okay", "Man, this sucks" };
                setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
                    @Override
                    public void call(ComboBoxList c, int id) {

                        GuiSettings.this.setPopup(null);
                        if (id < 0 || id >= vals.length)
                            return;

                        combo.setValue(vals[id]);

                    }
                }, this, combo, vals));
            }
        }
        if (element == combo2) {
            if (combo2.onClick(this)) {
                final Integer[] values = new Integer[32];
                for (int i = 0; i < 32; i++) {
                    values[i] = 1<<i;
                }
                setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
                    @Override
                    public void call(ComboBoxList c, int id) {

                        GuiSettings.this.setPopup(null);
                        if (id < 0 || id >= values.length)
                            return;

                        combo2.setValue(values[id]);

                    }
                }, this, combo2, values));
            }
        }
        if (element == back) {
            Game.instance.showGUI(new GuiMainMenu());
        }
        return true;
    }


}
