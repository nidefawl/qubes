package nidefawl.qubes.gui.controls;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.shader.Shaders;

public class ProgressBar extends AbstractUI {
    float                progress     = 0;
    float                lastProgress = 0;
    private FontRenderer fr;
    private String       s            = "";

    public ProgressBar() {
        this.fr = FontRenderer.get(null, 14, 0, 14);
        setProgress(1);
    }

    @Override
    public void update() {
        super.update();
        this.lastProgress = this.progress;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        Shaders.gui.enable();
        float w = this.width;
        float pr = this.lastProgress + (this.progress - this.lastProgress) * fTime;
        float pW = w * (pr);
        int xBar = this.posX;
        int yBar = this.posY;
        int zBar =0222;
        Engine.pxStack.push(0, 0, 2);
        this.round = 4;
        shadowSigma = 0.4f;
        extendx = 1;
        extendy = 1;
        int color1 = 0x44ff44;
        int color2 = 0x33dd33;  
//        Shaders.gui.
        renderRoundedBoxShadowInverse(xBar - 5, yBar, zBar - 4, w + 10, height, 0xdadada, 0.5f, true);
        this.round = 4;
        yBar+=2;
        resetShape();
        extendx = 3;
        renderRoundedBoxShadow(xBar, yBar, zBar, pW, height-4, color1, 0.7f, false);
        renderRoundedBoxShadow(xBar, yBar + height/2-3, zBar + 1, pW, height/2, -1, 0.3f, false);
        renderRoundedBoxShadow(xBar, yBar, zBar + 1, pW, (height/3)-1, color2, 0.2f, false);
        Shaders.textured.enable();
        Engine.pxStack.translate(0, 0, zBar + 2);
        fr.drawString(s, xBar + (w) / 2.0f, yBar + (height-fr.getLineHeight())/2 + fr.getLineHeight(), -1, true, 1.0f, 2);
        Engine.pxStack.pop();
        resetShape();
    }

    @Override
    public void initGui(boolean first) {
    }

    public void setProgress(float progress) {
        this.progress = Math.max(Math.min(progress, 1), 0);
        this.s = String.format("%.0f%%", Math.min(100, Math.max(0, progress*100)));
    }

    public void addProgress(float f) {
        setProgress(this.progress+f);
    }

    public float getProgress() {
        return this.progress;
    }

    public void setText(String string) {
        this.s = string;
    }
}