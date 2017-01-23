package nidefawl.qubes.gui;


import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.io.network.WorldInfo;
import nidefawl.qubes.network.client.ClientHandler;
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
    
    ScrollList scrolllist;
    private ArrayList<Button> list = Lists.newArrayList();
    private Button back;
    static int REQ_ID = 0;

    public GuiSelectWorld() {
    }

    @Override
    protected String getTitle() {
        return "Worlds";
    }
    @Override
    public void initGui(boolean first) {
        this.list.clear();
        scrolllist = new ScrollList(this);
        this.width = 430;
        this.posX = (Game.guiWidth-this.width)/2;
        this.posY = Game.guiHeight / 4;
        int scrollListHeight = 200;
        this.scrolllist.setSize(width-50, scrollListHeight);
        this.height = scrollListHeight+titleBarOffset+50;
        this.posY = Game.guiHeight/2-this.height/2;
        this.scrolllist.setPos(posX+25, posY+titleBarOffset);
        this.clearElements();
        {
            back = new Button(6, "Back");
            this.add(back);
            back.setSize(160, 30);
            back.setPos(this.width / 2 - 160/2, scrollListHeight+titleBarOffset+15);
        }
        this.add(this.scrolllist.scrollbarbutton);
        this.add(this.back);
        
    }

    public void fillList(ArrayList<WorldInfo> worldList) {
        this.clearElements();
        this.list.clear();
        int idx = 10;
        for (WorldInfo b : worldList) {
            WorldListControl c = new WorldListControl(idx++, b);
            list.add(c);
            System.err.println(b.name);
        }
        int y = 0;
        for (Button s : list) {
            s.setPos(0, y);
            s.setSize(this.scrolllist.width-14, 30);
            y += 36;
            scrolllist.add(s);
        }
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
