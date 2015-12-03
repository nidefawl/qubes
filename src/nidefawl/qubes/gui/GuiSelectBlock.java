package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.network.packet.PacketCSwitchWorld;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureUtil;

public class GuiSelectBlock extends Gui {

    final public FontRenderer font;
    private List<Stack> blocks;
    private Button fakeButton;
    private Stack sel;

    public GuiSelectBlock() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
    }

    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        {
            this.buttons.add(new Button(1, "Back"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height - 60);
            this.buttons.get(0).setSize(w, h);
            this.fakeButton = new Button(2, "NOOOO");
            this.buttons.add(this.fakeButton);
            this.fakeButton.draw = false;
            this.fakeButton.setPos(-1000, -1000);
            this.fakeButton.setSize(32, 32);
        }
        this.blocks = Lists.newArrayList();
        for (Block b : Block.getRegisteredBlocks()) {
            b.getItems(this.blocks);
        }
        Collections.sort(this.blocks, new Comparator<Stack>() {
            @Override
            public int compare(Stack s1, Stack s2) {
                int n= s1.getBlock().getClass().getName().compareToIgnoreCase(s2.getBlock().getClass().getName());
                if (n == 0) {
                    n= s1.getBlock().getName().compareToIgnoreCase(s2.getBlock().getName());
                }
                return n;
            }
        });
    }

    public void update() {
    }


    public void render(float fTime, double mX, double mY) {
        Shaders.colored.enable();
        Tess.instance.setColor(2, 128);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
        
        Shaders.singleblock.enable();
        Shaders.singleblock.setProgramUniform3f("in_offset", this.posX + this.width/2.0f, this.posY + this.height/2.0f, 0);
        Shaders.singleblock.setProgramUniform1f("in_scale", 2);
//        Shaders.singleblock.setProgramUniform3f("in_offset", 2,0, 0);
//        GL11.glDisable(GL_CULL_FACE);
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
//        Engine.blockDraw.drawBlock(Block.glowstone, 0);
//        glEnable(GL_CULL_FACE);
        
        Shaders.singleblock.enable();
        float blockscale = 1.0f;
        Shaders.singleblock.setProgramUniform1f("in_scale", blockscale);
        float bSize = 45;
        float g = 4;
        float out = 10;
        int perRow1 = Math.max(4, (int) ((this.width-50)/(bSize+out)));
        float xPos = this.width/2.0f-(perRow1*bSize+out)/2.0f;
        float yPos = xPos;
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
         sel = null;
         this.fakeButton.setPos(-1000, -1000);
         this.fakeButton.setSize(32, 32);
        int rendered = 0;
        int e = this.extendx;
        float r = this.round;
        this.round = 3;
        this.extendx = 0;
        for (int i = 0; i < blocks.size(); i++) {
            Stack stack = blocks.get(i);
            Block block = stack.getBlock();
            if (block != null) {
                float bX = this.posX+xPos + (rendered%perRow1)*(bSize+g);
                float bY = this.posY+yPos + (rendered/perRow1)*(bSize+g);
                boolean m = false;
                float pX1 = bX-bSize/2.0f;
                float pY1 = bY-bSize/2.0f;
                float pX2 = pX1+bSize;
                float pY2 = pY1+bSize;
                if (mX > pX1 && mX < pX2 && mY > pY1 && mY < pY2) {
                    m = true;
                    sel = stack;

                    fakeButton.setPos((int)pX1, (int)pY1);
                    fakeButton.setSize((int)(pX2-pX1), (int)(pY2-pY1));
                } else {
                }
                int zz = -30;

                {
                    float x = this.posX + this.width/2.0f;
                    float y = this.posY + this.height/2.0f;
                    int color = 0xc0c0c0;
                    if (m || stack.isEqualId(Game.instance.selBlock)) {
                        zz = -15;
                        pX1-=4;
                        pX2+=4;
                        pY2+=4;
                        pY1-=4;
                    }
                    Shaders.gui.enable();
                    renderRoundedBoxShadow(pX1, pY1, zz, pX2-pX1, pY2-pY1, color, 1, true);
                    Shaders.singleblock.enable();
                }
//                if (m || stack.isEqualId(Game.instance.selBlock)) {
//                    Shaders.colored.enable();
//                    Tess t = Tess.instance;
//                    if (m) {
//                        t.setColorF(-1, 0.4f);
//                    } else {
//                        t.setColorF(-1, 0.8f);
//                    }
//                    pX1--;
//                    pX2++;
//                    pY2++;
//                    pY1--;
//                    t.add(pX1, pY2);
//                    t.add(pX2, pY2);
//                    t.add(pX2, pY1);
//                    t.add(pX1, pY1);
//                    t.drawQuads();
//                    Shaders.singleblock.enable();
//                }
                if (m) {
                    Shaders.singleblock.setProgramUniform1f("in_scale", blockscale*1.3f);
                }
                Shaders.singleblock.setProgramUniform3f("in_offset", bX, bY, zz+20);
                Engine.blockDraw.drawBlock(block, stack.data, stack.getStackdata());
                if (m) {
                    Shaders.singleblock.setProgramUniform1f("in_scale", blockscale);
                }
                rendered++;    
            }
            
        }
        this.round = r;
        this.extendx = e;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Shader.disable();

        if (sel != null) {
            Shaders.textured.enable();
            font.drawString(""+sel.getBlock().getName(), this.width / 2, 50, -1, true, 1, 2);
            Shader.disable();
        }
        
//        Tess.instance.setColor(2, 255);
//        int x1 = this.width / 5*2;
//        int x2 = this.width - x1;
//        int y1 = this.height / 5*2;
//        int y2 = this.height - y1;
//        Tess.instance.add(x1, y2);
//        Tess.instance.add(x2, y2);
//        Tess.instance.add(x2, y1);
//        Tess.instance.add(x1, y1);
//        Tess.instance.draw(GL_QUADS);
        super.renderButtons(fTime, mX, mY);

    }

    public boolean onGuiClicked(AbstractUI element) {
        if (element.id == 1) {
            Game.instance.showGUI(null);
        }
        if (element.id == 2 && this.sel != null) {
            Game.instance.selBlock = sel;
            Game.instance.showGUI(null);
        }
        return true;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (super.onKeyPress(key, scancode, action, mods)) {
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int world = key - GLFW.GLFW_KEY_1;
            Game.instance.sendPacket(new PacketCSwitchWorld(world));
            Game.instance.showGUI(null);
        }
        return true;
    }
}
