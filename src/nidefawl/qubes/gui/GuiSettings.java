package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ComboBox;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;

public class GuiSettings extends Gui {
    static int nextID = 3;

    static class Setting {
        Object[] vals;
        ComboBox box;

        /**
         * @param string
         * @param g 
         * @param string2
         */
        public Setting(Gui g, String string, Object current, Object[] vals) {
            this.box = new ComboBox(g, nextID++, string);
            this.box.setValue(current);
            this.vals = vals;
        }

        void callback(int id) {
        };
    }

    final public FontRenderer font;
    private Button            back;
    List<Setting>             list = Lists.newArrayList();
    private Setting testSetting;
    private Setting distanceSetting;
    private Setting shadowSetting;
    private Setting reflectionSetting;
    private Setting smaaSetting;
    private Setting smaaQSetting;
    private Setting aoSetting;

    public GuiSettings() {
        this.font = FontRenderer.get(null, 18, 0, 20);
    }

    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        this.list.clear();
        int w1 = 160;
        int h = 30;
        List<String> l = Lists.newArrayList();
        l.addAll(Arrays.asList(new String[] { "Awesome!!!", "Well, pretty Okay", "Man, this sucks" }));
        for (int j = 0; j < 20; j++) {
            l.add("Option "+(j+1));
        }
        String[] arr = l.toArray(new String[l.size()]);
        list.add((this.testSetting = new Setting(this, "String test", "Please pick", arr) {
            void callback(int id) {

            }
        }));
        List<Integer> clist = Lists.newArrayList();
        for (int i = 4; i <= 24; i++) {
            clist.add(i);
        }
        final Integer[] values = clist.toArray(new Integer[clist.size()]);
        list.add((this.distanceSetting = new Setting(this, "Chunk load distance", Game.instance.settings.chunkLoadDistance, values) {
            void callback(int id) {
                Game.instance.settings.chunkLoadDistance = values[id];
                Engine.regionRenderer.init();
                Game.instance.saveSettings();
            }
        }));
        final String[] shadowSettings = new String[] { "Basic", "Detailed" };
        list.add((this.shadowSetting = new Setting(this, "Shadows", shadowSettings[Game.instance.settings.shadowDrawMode & 1], shadowSettings) {
            void callback(int id) {
                Game.instance.settings.shadowDrawMode = id;
                Engine.shadowRenderer.init();
                Game.instance.saveSettings();
            }
        }));
        final String[] reflections = new String[] { "Disabled", "Basic", "Detailed", "Can't play" };
        list.add((this.reflectionSetting = new Setting(this, "Reflections", reflections[Game.instance.settings.ssr & 3], reflections) {
            void callback(int id) {
                Game.instance.settings.ssr = id;
                Engine.outRenderer.setSSR(id);
                Game.instance.saveSettings();
            }
        }));
        final String[] strAOSettings = new String[] { "Disabled", "Enabled" };
        list.add((this.aoSetting = new Setting(this, "Ambient Occlusion", strAOSettings[Game.instance.settings.ao & 1], strAOSettings) {
            void callback(int id) {
                Game.instance.settings.ao = id;
                Engine.outRenderer.initAO(Game.displayWidth, Game.displayHeight);
                UniformBuffer.rebindShaders(); // For some stupid reason we have to rebind
                ShaderBuffer.rebindShaders(); // For some stupid reason we have to rebind
            }
        }));
        final String[] smaa = new String[] { "Disabled", "1x SMAA" };
        list.add((this.smaaSetting = new Setting(this, "Anti-Aliasing", smaa[Game.instance.settings.aa & 1], smaa) {
            void callback(int id) {
                Game.instance.settings.aa = id;
                Game.instance.saveSettings();
                Engine.outRenderer.initAA();
            }
        }));

        final String[] smaaQ = SMAA.qualDesc;
        list.add((this.smaaQSetting = new Setting(this, "SMAA Quality", smaaQ[Game.instance.settings.smaaQuality%smaaQ.length], smaaQ) {
            void callback(int id) {
                Game.instance.settings.smaaQuality = id;
                Game.instance.saveSettings();
                Engine.outRenderer.initAA();
            }
        }));
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
        if(Game.instance.isConnected())
            renderBackground(fTime, mX, mY, true, 0.8f);
        else
            renderBackground(fTime, mX, mY, true, 1.0f);
        Shaders.textured.enable();
        this.font.drawString("Settings", this.posX + this.width / 2.0f, this.posY + this.height / 6, -1, true, 1.0f);
        this.smaaQSetting.box.enabled = Game.instance.settings.aa==1;
        
        // Disable non-runtime options
        if(Game.instance.isConnected()) {
            this.distanceSetting.box.enabled = false;
        }
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
            if(!Game.instance.isConnected())
                Game.instance.showGUI(new GuiMainMenu());
            else
                Game.instance.showGUI(null);
        }
        return true;
    }

}
