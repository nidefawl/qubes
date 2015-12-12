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
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.network.packet.PacketCSwitchWorld;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameMath;

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

    float dir     = 0.2f;
    float rot     = 0;
    float lastRot = 0;
    public void update() {
        this.lastRot = rot;
        this.rot += 4*GameMath.PI_OVER_180;
        if (this.rot>360*GameMath.PI_OVER_180) {
            this.lastRot -= 360*GameMath.PI_OVER_180;
            this.rot -= 360*GameMath.PI_OVER_180;
        }
    }


    public void render(float fTime, double mX, double mY) {
        float animRot = lastRot+(rot-lastRot)*fTime;
        
        Shaders.colored.enable();
        Tess.instance.setColor(2, 128);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
        
//        Shaders.singleblock.setProgramUniform3f("in_offset", 2,0, 0);
//        GL11.glDisable(GL_CULL_FACE);
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
//        Engine.blockDraw.drawBlock(Block.glowstone, 0);
//        glEnable(GL_CULL_FACE);

        float bSize = 44;
        float bascale = (bSize/32.0f)*0.6f;
        float g = 4;
        int perRow1 = Math.max(4, (int) ((this.width-220)/(bSize+g)));
        int rows = 1+(blocks.size()/perRow1);
        float xPos = 0 + (this.width-perRow1*(bSize+g))/2.0f;
        float yPos = Math.max(50, 0 + (this.height-rows*(bSize+g))/2.0f);
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
         sel = null;
         this.fakeButton.setPos(-1000, -1000);
         this.fakeButton.setSize((int)bSize-10, (int)bSize-10);
        int rendered = 0;
        int e = this.extendx;
        float r = this.round;
        this.round = 5;
        this.extendx = 0;
        float sX, sZ;
        sX = sZ = 0;
        int color = 0xc0c0c0;
        for (int i = 0; i < blocks.size(); i++) {
            Stack stack = blocks.get(i);
            Block block = stack.getBlock();
            if (block != null) {
                float bX = this.posX+xPos+bSize/2.0f + (rendered%perRow1)*(bSize+g);
                float bY = this.posY+yPos+bSize/2.0f+ (rendered/perRow1)*(bSize+g);
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
                    sX = bX;
                    sZ = bY;
                }
                int zz = -200;
                int z = 0;
                float x = this.posX + this.width/2.0f;
                float y = this.posY + this.height/2.0f;
                Shaders.gui.enable();
                renderRoundedBoxShadow(pX1, pY1, zz, pX2-pX1, pY2-pY1, color, 1, true);
                float fRot = block.getInvRenderRotation();
                float blockscale = bascale;
                int renderData = block.getInvRenderData(stack);
                Engine.blockDraw.setOffset(bX, bY, zz+100);
                Engine.blockDraw.setScale(blockscale);
                Engine.blockDraw.setRotation(15, 90+45+fRot*GameMath.P_180_OVER_PI, 0);
                Engine.blockDraw.drawBlock(block, renderData, stack.getStackdata());
                rendered++;    
            }
            
        }
        if (this.sel != null) {
            Block block = this.sel.getBlock();
            float pX1 = sX-bSize/2.0f;
            float pY1 = sZ-bSize/2.0f;
            float pX2 = pX1+bSize;
            float pY2 = pY1+bSize;
            float zz = -50;
            float offset = bSize*0.9f;
            float fRot = block.getInvRenderRotation();
            float blockscale = 1.333f;
            fRot+=animRot;
            blockscale*=2.0f;
            int renderData = block.getInvRenderData(this.sel);
            Shaders.gui.enable();
            pX1-=offset;
            pX2+=offset;
            pY2+=offset;
            pY1-=offset;
            renderRoundedBoxShadow(pX1, pY1, zz, pX2-pX1, pY2-pY1, color, 1, true);
            pX1-=4;
            pX2+=4;
            pY2+=4;
            pY1-=4;
            renderRoundedBoxShadow(pX1, pY1, zz, pX2-pX1, pY2-pY1, color, 0.5f, true);
            Engine.blockDraw.setOffset(sX, sZ, zz+100);
            Engine.blockDraw.setScale(blockscale);
            Engine.blockDraw.setRotation(15, 90+45+fRot*GameMath.P_180_OVER_PI, 0);
            Engine.blockDraw.drawBlock(block, renderData, this.sel.getStackdata());
            rendered++;    
        }
        this.round = r;
        this.extendx = e;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Shader.disable();

        if (sel != null) {
            Shaders.textured.enable();
            font.drawString(""+sel.getBlock().getName(), this.width / 2, Math.max(30, yPos-50), -1, true, 1, 2);
            Shader.disable();
        }
        
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
