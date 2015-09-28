package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ComboBox;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.shader.Shaders;

public class GuiSettings extends Gui {
    static int nextID = 3;

    static class Setting {
        Object[] vals;
        ComboBox box;

        /**
         * @param string
         * @param string2
         */
        public Setting(String string, Object current, Object[] vals) {
            this.box = new ComboBox(nextID++, string);
            this.box.setValue(current);
            this.vals = vals;
        }

        /**
         * 
         */
        public Setting() {
            // TODO Auto-generated constructor stub
        }

        void callback(int id) {
        };
    }

    final public FontRenderer font;
    private Button            back;
    List<Setting>             list = Lists.newArrayList();

    public GuiSettings() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
    }

    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        int w1 = 160;
        int h = 30;
        list.add(new Setting("String test", "Please pick", new String[] { "Awesome!!!", "Well, pretty Okay", "Man, this sucks" }) {
            void callback(int id) {

            }
        });
        List<Integer> clist = Lists.newArrayList();
        for (int i = 4; i <= 24; i++) {
            clist.add(i);
        }
        final Integer[] values = clist.toArray(new Integer[clist.size()]);
        list.add(new Setting("Chunk load distance", Game.instance.settings.chunkLoadDistance, values) {
            void callback(int id) {
                Game.instance.settings.chunkLoadDistance = values[id];
                Game.instance.saveSettings();
            }
        });
        final String[] shadowSettings = new String[] { "Basic", "Detailed" };
        list.add(new Setting("Shadows", shadowSettings[Game.instance.settings.shadowDrawMode & 1], shadowSettings) {
            void callback(int id) {
                Game.instance.settings.shadowDrawMode = id;
                Engine.regionRenderer.init();
                Game.instance.saveSettings();
            }
        });
        final String[] reflections = new String[] { "Disabled", "Basic", "Detailed", "Can't play" };
        list.add(new Setting("Reflections", reflections[Game.instance.settings.ssr & 3], reflections) {
            void callback(int id) {
                Game.instance.settings.ssr = id;
                Engine.outRenderer.setSSR(id);
                Game.instance.saveSettings();
            }
        });
        int left = this.posX + this.width / 2 - w1 / 2;
        int y = this.posY + this.height / 6 + 40;
        for (Setting s : list) {
            s.box.setPos(left, y);
            s.box.setSize(w1, h);
            s.box.titleWidth = 185;
            y += 45;
            this.buttons.add(s.box);
        }
        {
            back = new Button(6, "Back");
            this.buttons.add(back);
            back.setPos(left, this.posY + this.height / 2 + 120);
            back.setSize(w1, h);
        }
    }

    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action);
    }

    public void update() {

    }

    public void render(float fTime, double mX, double mY) {
        this.font.drawString("Settings", this.posX + this.width / 2.0f, this.posY + this.height / 6, -1, true, 1.0f);
        Shaders.colored.enable();
        Tess.instance.setColor(2, 255);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
        //        Shaders.textured.enable();
        //        Shader.disable();
        super.renderButtons(fTime, mX, mY);

    }

    public boolean onGuiClicked(AbstractUI element) {
        for (int i = 0; i < this.list.size(); i++) {
            if (this.list.get(i).box == element) {
                final Setting s = this.list.get(i);
                if (s.box.onClick(this)) {
                    setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
                        @Override
                        public void call(ComboBoxList c, int id) {

                            GuiSettings.this.setPopup(null);
                            if (id < 0 || id >= s.vals.length)
                                return;
                            s.box.setValue(s.vals[id]);
                            s.callback(id);
                        }
                    }, this, s.box, s.vals));
                }
            }
        }
        if (element == back) {
            Game.instance.showGUI(new GuiMainMenu());
        }
        return true;
    }

}
