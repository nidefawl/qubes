package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.shader.Shaders;

public class GuiInventory2 extends GuiInventoryBase {

    private Button btn1;
    private Button btn2;
    float progress = 0;
    int ticksDone = 0;

    private void start() {
        running = true;
        progress = 0;
        ticksDone=0;
    }
    private void end() {
        running = false;
        progress = 0;
        ticksDone=0;
    }
    boolean running;
    private float lastProgress;
    
    public GuiInventory2() {
    }
    public String getTitle() {
        return "Crafting";
    }
    @Override
    public void initGui(boolean first) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            close();
            return;
        }
        this.slots = p.getSlots(1);

        int bw = 60;
//        if (this.bounds != null) {
//            setPos(this.bounds[0], this.bounds[1]);
//            setSize(this.bounds[2], this.bounds[3]);
//        } else {
            int rows = (this.slots.getSlots().size()-1)/8;
            int width = 20 + (slotBDist+slotW)*8 + bw + 20;
            int height = titleBarHeight + 15+ (slotBDist+slotW)*rows + 30;
            int xPos = (Game.displayWidth-width)/2;
            int yPos = (Game.displayHeight-height)/2;
            setPos(xPos, yPos);
            setSize(width, height);
//        }
        this.buttons.clear();
        btn1 = new Button(1, "Start");
        btn2 = new Button(2, "Stop");
        this.buttons.add(btn1);
        this.buttons.add(btn2);
        btn1.setPos(width-bw-14, height-34-20);
        btn1.setSize(bw, 20);
        btn1.round = 3;
        btn2.setPos(width-bw-14, height-30);
        btn2.setSize(bw, 20);
        btn2.round = 3;
    }
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        if (element == btn1) {
            end();
            start();
            return true;
        }
        if (element == btn2) {
            end();
            return true;
        }
        return super.onGuiClicked(element);
    }

    public void update() {
        this.lastProgress = progress;
        if (running) {
            if (progress < 100) {
                this.progress++;
                if (this.progress >= 100) {
                    
                }
            } else {
                ticksDone++;
                if (ticksDone > 11000) {
                    end();
                }
            }
        }
    }

    public void render(float fTime, double mX, double mY) {
        Shaders.colored.enable();
        
        renderSlots(fTime, mX, mY);
        Shaders.gui.enable();
        float w = this.width-20-90;
        float pr = this.lastProgress+(this.progress-this.lastProgress)*fTime;
        float pW = w*(pr/100.0f);
        int xBar = this.posX+14;
        int yBar = this.posY+this.height-30;
        int zBar = 4;
        this.round = 4;
        this.shadowSigma=1;
        shadowSigma = 0.4f;
        extendx = 1;
        extendy = 1;
        int color1 = 0x44ff44;
        int color2 = 0x33dd33; 
        renderRoundedBoxShadowInverse(xBar-5, yBar-2, zBar-4, w+10, 24, 0xdadada, 0.8f, true);
        resetShape();
        this.round = 4;
        renderRoundedBoxShadow(xBar, yBar, zBar, pW, 20, color1, 0.7f, false);
        renderRoundedBoxShadow(xBar, yBar+8, zBar+1, pW, 12, -1, 0.3f, false);
        renderRoundedBoxShadow(xBar, yBar, zBar+1, pW, 7, color2, 0.2f, false);
        Shaders.textured.enable();
        String s = String.format("%.0f%%", Math.min(100, Math.max(0, progress)));
        Engine.pxStack.push(0, 0, zBar+3);
        FontRenderer fr = FontRenderer.get(null, 14, 0, 18);
        fr.drawString(s, xBar+w/2.0f-16, yBar+fr.getLineHeight(), -1, true, 1.0f);
        Engine.pxStack.pop();
        resetShape();
        super.renderButtons(fTime, mX, mY);
    }

}
