package nidefawl.qubes.gui;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.glGetFloat;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.config.ClientSettings;
import nidefawl.qubes.config.RenderSettings;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ComboBox;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.ShaderBuffer;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.array.BlockTextureArray;

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
            this.box.titleLeft=true;
            this.vals = vals;
        }

        void callback(int id) {
        };
    }

    private Button            back;
    List<Setting>             list = Lists.newArrayList();
    private Setting gui3dSetting;
    private Setting distanceSetting;
    private Setting shadowSetting;
    private Setting reflectionSetting;
    private Setting smaaSetting;
    private Setting smaaQSetting;
    private Setting smaaPSetting;
    private Setting aoSetting;
    private Setting normalMappingSetting;
    private Setting anisotropySetting;

    final ClientSettings settings;
    final RenderSettings renderSettings;
    public GuiSettings(Gui parent) {
        this(parent, Game.instance.settings.renderSettings, Game.instance.settings);
    }
    public GuiSettings(Gui parent, RenderSettings renderSettings, ClientSettings settings) {
        this.parent = parent;
        this.renderSettings = renderSettings;
        this.settings = settings;
    }

    @Override
    public void initGui(boolean first) {
        this.width = 430;
        this.clearElements();
        this.list.clear();
        final String[] str3dSettings = new String[] { "VR Only", "Always On" };
        list.add((this.gui3dSetting = new Setting(this, "Render 3D GUI", str3dSettings[settings.gui3d ? 1 : 0], str3dSettings) {
            void callback(int id) {
                settings.gui3d = id == 1;
                Game.instance.saveSettings();
                Game.instance.updateGui3dMode();
                Game.instance.showGUI(new GuiSettings(null));
            }
        }));
        int w1 = 160;
        int h = 30;
        List<Integer> clist = Lists.newArrayList();
        for (int i = 4; i <= 24; i++) {
            clist.add(i);
        }
        final Integer[] values = clist.toArray(new Integer[clist.size()]);
        list.add((this.distanceSetting = new Setting(this, "Chunk load distance", settings.chunkLoadDistance, values) {
            void callback(int id) {
                settings.chunkLoadDistance = values[id];
                Engine.regionRenderer.init();
                Game.instance.saveSettings();
            }
        }));
        final String[] shadowSettings = new String[] { "Basic", "Detailed" };
        list.add((this.shadowSetting = new Setting(this, "Shadows", shadowSettings[renderSettings.shadowDrawMode & 1], shadowSettings) {
            void callback(int id) {
                renderSettings.shadowDrawMode = id;
                Engine.shadowRenderer.init();
                Game.instance.saveSettings();
                Engine.regionRenderer.reRender();
            }
        }));
        final String[] reflections = new String[] { "Disabled", "Basic", "Detailed", "Ultra" };
        list.add((this.reflectionSetting = new Setting(this, "Reflections", reflections[renderSettings.ssr & 3], reflections) {
            void callback(int id) {
                renderSettings.ssr = id;
                Engine.outRenderer.setSSR(id);
                Game.instance.saveSettings();
                Engine.outRenderer.resize(Game.guiWidth, Game.guiHeight);
            }
        }));
        final String[] strAOSettings = new String[] { "Disabled", "Enabled" };
        list.add((this.aoSetting = new Setting(this, "Ambient Occlusion", strAOSettings[renderSettings.ao & 1], strAOSettings) {
            void callback(int id) {
                renderSettings.ao = id;
                Engine.outRenderer.initAO();
                Engine.outRenderer.initShaders();
                UniformBuffer.rebindShaders(); // For some stupid reason we have to rebind
                ShaderBuffer.rebindShaders(); // For some stupid reason we have to rebind
            }
        }));
        list.add((this.normalMappingSetting = new Setting(this, "Normal Mapping", strAOSettings[renderSettings.normalMapping & 1], strAOSettings) {
            void callback(int id) {
                renderSettings.normalMapping = id;
                Engine.worldRenderer.initShaders();
            }
        }));
        final String[] smaa = new String[] { "Disabled", "1x SMAA" };
        list.add((this.smaaSetting = new Setting(this, "Anti-Aliasing", smaa[renderSettings.aa & 1], smaa) {
            void callback(int id) {
                renderSettings.aa = id;
                Game.instance.saveSettings();
                Engine.outRenderer.initAA();
            }
        }));

        final String[] smaaQ = SMAA.qualDesc;
        list.add((this.smaaQSetting = new Setting(this, "SMAA Quality", smaaQ[renderSettings.smaaQuality%smaaQ.length], smaaQ) {
            void callback(int id) {
                renderSettings.smaaQuality = id;
                Game.instance.saveSettings();
                Engine.outRenderer.initAA();
            }
        }));
        final String[] smaaP = new String[] { "Disabled", "Enabled" };
        list.add((this.smaaPSetting = new Setting(this, "AA Pred. Threshold", smaaP[renderSettings.smaaPredication?1:0], smaaP) {
            void callback(int id) {
                renderSettings.smaaPredication = id > 0;
                Game.instance.saveSettings();
                Engine.outRenderer.initAA();
                Engine.outRenderer.initShaders();
            }
        }));
        List<String> alist = Lists.newArrayList();
        alist.add("Disabled");
        final float f = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        int nf = 2;
        while (nf <= f) {
            alist.add("" + nf+"x");
            nf *= 2;
        }
        String curValue = renderSettings.anisotropicFiltering == 0 ? "Disabled" : (""+renderSettings.anisotropicFiltering+"x");
        final String[] avalues = alist.toArray(new String[alist.size()]);
        list.add((this.anisotropySetting = new Setting(this, "Anisotropic Filtering", curValue, avalues) {
            void callback(int id) {
                int level = 0;
                if (id > 0) {
                    level = 2;
                    while (level <= f&&--id>0) {
                        level *= 2;
                    }
                }
                renderSettings.anisotropicFiltering = level;
                Game.instance.saveSettings();
                BlockTextureArray.getInstance().setAnisotropicFiltering(level);
                BlockTextureArray.getInstance().reload();// make sync task?
            }
        }));
        int left = this.width / 2+15;
        int y = titleBarOffset;
        for (Setting s : list) {
            s.box.setPos(left, y);
            s.box.setSize(w1, h);
            s.box.titleWidth = 205;
            y += 45;
            this.add(s.box);
        }
        {
             left = this.width / 2 - (w1) / 2;
            back = new Button(6, "Back");
            this.add(back);
            back.setSize(w1, h);
            back.setPos(left, y+5);
        }
        this.height = back.posY+back.height+10;
        this.posX = (Game.guiWidth-this.width)/2;
        this.posY = (Game.guiHeight-this.height) / 2;
    }

    protected String getTitle() {
        return "Settings";
    }
    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
//        Shaders.textured.enable();
//        this.font.drawString("Settings", this.posX + this.width / 2.0f, this.posY + 5, -1, true, 1.0f, 2);
        this.smaaQSetting.box.enabled = renderSettings.aa==1;
        
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
            Game.instance.showGUI((Gui) parent);
        }
        return true;
    }

}
