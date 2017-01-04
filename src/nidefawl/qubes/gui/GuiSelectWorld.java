package nidefawl.qubes.gui;


import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.io.network.DataListType;
import nidefawl.qubes.io.network.WorldInfo;
import nidefawl.qubes.network.client.ClientHandler;
import nidefawl.qubes.network.packet.PacketCListRequest;
import nidefawl.qubes.network.packet.PacketCSwitchWorld;
import nidefawl.qubes.shader.Shaders;

public class GuiSelectWorld extends Gui {
    static class WorldListControl extends Button {

        private WorldInfo info;

        public WorldListControl(int id, WorldInfo info) {
            super(id, "World " + info.name + "");
            this.info = info;
        }

        @Override
        public void render(float fTime, double mX, double mY) {
            super.render(fTime, mX, mY);
            Shaders.colored.enable();
            Tess tessellator = Tess.instance;
            GL11.glLineWidth(1.0F);
            int w = this.width;
            int h = this.height;

            //          GL11.glBegin(GL11.GL_LINE_STRIP);
            tessellator.setColorF(-1, 0.1f);

            tessellator.add(this.posX + w - 12, this.posY + h + 3);
            tessellator.setColorF(-1, 0.4f);
            tessellator.add(this.posX + 2 + (w - 12) / 2, this.posY + h + 3);
            tessellator.setColorF(-1, 0.1f);

            tessellator.add(this.posX + 2, this.posY + h + 3);
            tessellator.draw(GL11.GL_LINE_STRIP);
        }

        @Override
        public void initGui(boolean first) {
        }
    }
    final public FontRenderer font;
    ScrollList scrolllist;
    private ArrayList<Button> list = Lists.newArrayList();
    private Button btnBack;
    static int REQ_ID = 0;

    public GuiSelectWorld() {
        this.font = FontRenderer.get(0, 18, 0);
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        {
            this.btnBack = new Button(1, "Back");
            int w = 200;
            int h = 30;
            this.btnBack.setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height / 6*5);
            this.btnBack.setSize(w, h);
        }
        scrolllist = new ScrollList(this);

        this.list.clear();
        this.add(this.btnBack);
        
    }

    public void fillList(ArrayList<WorldInfo> worldList) {

        this.clearElements();
        this.list.clear();
        int w1 = 520;
        int idx = 10;
        for (WorldInfo b : worldList) {
            WorldListControl c = new WorldListControl(idx++, b);
            list.add(c);
            System.err.println(b.name);
        }
        int left = this.width / 2 - (w1 / 2);
        this.scrolllist.setPos(left, this.height / 6 + 40);
        int y = 0;
        for (Button s : list) {
            s.setPos(0, y);
            s.setSize(w1 - 14, 30);
            y += 36;
            scrolllist.add(s);
        }
        this.scrolllist.setSize(w1, this.height / 2);
        this.add(this.scrolllist.scrollbarbutton);
        this.add(this.btnBack);
    }
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action) || this.scrolllist.onMouseClick(button, action);
    }

    public void render(float fTime, double mX, double mY) {
        ClientHandler client = Game.instance.getClientHandler();
        if (client != null && this.list.isEmpty() && !client.worldList.isEmpty()) {
            fillList(client.worldList);
        }

        renderBackground(fTime, mX, mY, true, 0.7f);
        Shaders.textured.enable();
        this.font.drawString("Worlds", this.width / 2.0f, this.height / 6, -1, true, 1.0f, 2);
        this.scrolllist.render(fTime, mX, mY);

        super.renderButtons(fTime, mX, mY);

    }

    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            System.out.println(Game.instance.getWorld());
            Game.instance.showGUI(Game.instance.getWorld()==null?new GuiMainMenu():null);
        }
        if (element instanceof WorldListControl) {
            WorldListControl list = (WorldListControl) element;
            Game.instance.sendPacket(new PacketCSwitchWorld(list.info.id));
            Game.instance.showGUI(null);
        }
        return true;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (super.onKeyPress(key, scancode, action, mods)) {
            return true;
        }
        return true;
    }
    public boolean onWheelScroll(double xoffset, double yoffset) {
        this.scrolllist.onWheelScroll(xoffset, yoffset);
        return true;
    }
    public void update() {
        super.update();
        this.scrolllist.update();
    }
}
