package nidefawl.qubes.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;

public class GuiSelectBlock extends Gui {

    private List<BlockStack> blocks;
    private Button fakeButton;
    private BlockStack sel;

    public GuiSelectBlock() {
    }
    
    @Override
    public void initGui(boolean first) {
        this.clearElements();
        {
            this.add(new Button(1, "Back"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX + this.width / 2 - w / 2, this.posY + this.height - 60);
            this.buttons.get(0).setSize(w, h);
            this.fakeButton = new Button(2, "NOOOO");
            this.add(this.fakeButton);
            this.fakeButton.draw = false;
            this.fakeButton.setPos(-1000, -1000);
            this.fakeButton.setSize(32, 32);
        }
        this.blocks = Lists.newArrayList();
        for (Block b : Block.getRegisteredBlocks()) {
            b.getItems(this.blocks);
        }
//        Collections.sort(this.blocks, new Comparator<BlockStack>() {
//            @Override
//            public int compare(BlockStack s1, BlockStack s2) {
//                Block a = s1.getBlock();
//                Block b = s2.getBlock();
//                int gid1 = -1;
//                int gid2 = -1;
//                BlockGroup g1 = a.getBlockGroup();
//                BlockGroup g2 = b.getBlockGroup();
//                if (g1 != null) {
//                    gid1 = g1.getId();
//                }
//                if (g2 != null) {
//                    gid2 = g2.getId();
//                }
//                int n = Integer.compare(gid1, gid2);
//                if (n != 0)
//                    return n;
//                n= a.getClass().getName().compareToIgnoreCase(b.getClass().getName());
//                if (n == 0) {
//                    n= a.getName().compareToIgnoreCase(b.getName());
//                }
//                return n;
//            }
//        });
    }

    float dir     = 0.2f;
    float rot     = 0;
    float lastRot = 0;
    public void update() {
        super.update();
        this.lastRot = rot;
        this.rot += 4*GameMath.PI_OVER_180;
        if (this.rot>360*GameMath.PI_OVER_180) {
            this.lastRot -= 360*GameMath.PI_OVER_180;
            this.rot -= 360*GameMath.PI_OVER_180;
        }
    }


    public void render(float fTime, double mX, double mY) {
        float animRot = lastRot+(rot-lastRot)*fTime;
////        System.out.println(mX+"/"+mY);
//        mX=630;
//        mY=670;
        Engine.setPipeStateColored2D();
        ITess tess = Engine.getTess();
        tess.setColor(2, 128);
        float zBG = -170;
        tess.add(this.posX, this.posY + this.height, zBG);
        tess.add(this.posX + this.width, this.posY + this.height, zBG);
        tess.add(this.posX + this.width, this.posY, zBG);
        tess.add(this.posX, this.posY, zBG);
        tess.drawQuads();
        

        float bSize = 32;
        float bascale = (bSize/32.0f)*0.6f;
        float g = 4;
        int perRow1 = Math.max(4, (int) ((this.width-220)/(bSize+g)));
        int rows = 1+(blocks.size()/perRow1);
        float xPos = 0 + (this.width-perRow1*(bSize+g))/2.0f;
        float yPos = Math.max(50, 0 + (this.height-rows*(bSize+g))/2.0f);
        
         sel = null;
         this.fakeButton.setPos(-1000, -1000);
         this.fakeButton.setSize((int)bSize-10, (int)bSize-10);
        this.round = 5;
        this.extendx = 0;
        float sX, sZ;
        sX = sZ = 0;
        int color = 0xc0c0c0;
        for (int pass = 0; pass < 2; pass++) {
            int rendered = 0;
            if (pass == 0) {
            } else {
            }
            for (int i = 0; i < blocks.size(); i++) {
                BlockStack stack = blocks.get(i);
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
                    int zz = -120;
                    int z = 0;
                    float x = this.posX + this.width/2.0f;
                    float y = this.posY + this.height/2.0f;
                    if (pass == 0) {
                        renderRoundedBoxShadow(pX1, pY1, zz, pX2-pX1, pY2-pY1, color, 1, true);
                    } else {
                        float blockscale = bascale;
                        int renderData = block.getInvRenderData(stack);
                        Engine.blockDraw.setOffset(bX, bY, zz+100);
                        Engine.blockDraw.setScale(blockscale);
                        Engine.blockDraw.drawBlockDefault(block, renderData, stack.getStackdata());
                    }  
                    rendered++;  
                }
    
            }
        }
        super.renderButtons(fTime, mX, mY);
        if (!Engine.isVulkan)
        GL11.glEnable(GL11.GL_DEPTH_TEST);
//        Engine.pxStack.push(0, 0, 420);
        if (this.sel != null) {
            Block block = this.sel.getBlock();
             bSize = 96;
             bascale = (bSize/32.0f)*0.6f;
             float offset = bSize*0.5f;
            float pX1 = sX+bSize/2.0f;
            float pY1 = sZ+bSize/2.0f;
            boolean topDown = false;
            if (pY1+bSize+offset+38 > Engine.getGuiHeight()) {
                pY1 = sZ-bSize-bSize/2.0f;
//                pY1 = (Game.displayHeight) - bSize*2+offset+32;
                pY1-=16;
                topDown = true;
            } else {
                pY1+=16;
            }
            if (pX1+bSize+offset > Engine.getGuiWidth()) {
                pX1 = sX-bSize-bSize/2.0f;
                pX1-=16;
            } else {
                pX1+=16;
            }
            float pX2 = pX1+bSize;
            float pY2 = pY1+bSize;
            float fRot = block.getInvRenderRotation();
            float blockscale = bascale;
            fRot+=animRot;
            blockscale*=2.0f;
            int renderData = block.getInvRenderData(this.sel);
            pX1-=offset;
            pX2+=offset;
            pY2+=offset;
            pY1-=offset;
            Engine.pxStack.push(0, 0, 60);
//            renderRoundedBoxShadow(pX1, pY1, -5, pX2-pX1, pY2-pY1, color, 0.8f, true);
            if (sel != null) {
                String s = sel.getBlock().getName();
                float w = font.getStringWidth(s)+10;
                float extraw=pX2-pX1<w?(w-(pX2-pX1))/2:0;
                int h = 28;
                int yPosText = (int) (topDown?pY1-h-2:pY2+6);
                pX1-=4;
                pX2+=4;
                pY2+=4;
                pY1-=4;
                renderRoundedBoxShadow(pX1-extraw, yPosText, -3, pX2-pX1+extraw*2, h, color, 0.8f, true);
                font.drawString(""+sel.getBlock().getName(), pX1+(pY2-pY1)/2.0f, yPosText+h-5, -1, true, 1, 2);
            } else {

                pX1-=4;
                pX2+=4;
                pY2+=4;
                pY1-=4;
            }
            renderRoundedBoxShadow(pX1, pY1, 0, pX2-pX1, pY2-pY1, color, 0.7f, true);
            Engine.blockDraw.setOffset(pX1+bSize, pY1+bSize, 0);
            Engine.blockDraw.setScale(blockscale);
            Engine.blockDraw.setRotation(15, 90+45+fRot*GameMath.P_180_OVER_PI, 0);
            Engine.blockDraw.drawBlock(block, renderData, this.sel.getStackdata());
          Engine.pxStack.pop();
        }
        if (!Engine.isVulkan)
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        resetShape();
        

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
}
