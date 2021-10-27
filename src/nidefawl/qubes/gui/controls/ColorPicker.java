package nidefawl.qubes.gui.controls;


import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.util.Color;

public abstract class ColorPicker extends AbstractUI {
    private Button[] colorPick;
    float valH=0;
    float valS=1;
    float valL=0.5f;
    private int rgb;
    final int btnWH = 16;
    final int btnStep = btnWH+8;
    int boxW = btnStep*3-10;
    final int extendSize = 24;
    int spacing = 20;
    public ColorPicker(Gui parent) {
        this.parent = parent;
        valH = 0.7f;
        valS = 0.8f;
        valL = 0.25f;
        rgb = Color.HSLtoRGB(1-valH, valS, valL);

        
    }
    public void setRgb(int rgb2) {
        float[] hsl = Color.RGBtoHSL((rgb2>>16)&0xFF, (rgb2>>8)&0xFF, rgb2&0xFF, null);
        valH = 1-hsl[0];
        valS = hsl[1];
        valL = hsl[2];
        rgb = Color.HSLtoRGB(1-valH, valS, valL);
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        int pickIdx = -1;
        for (int i = 0; i < 3; i++) {
            if (colorPick[i] == selectedButton) {
                pickIdx = i; break;
            }
        }
        int wRight = this.width-boxW-spacing;
        int xRight = this.posX+boxW+spacing;
        double bWidth = colorPick[0].width;
        if (pickIdx > -1) {
            double pos = (mX-(xRight-extendSize/2)-bWidth/2.0f)/(double)(wRight+(extendSize)-bWidth);
            if (Mouse.isButtonDown(1)) {
                pos = ((int)(pos*10)/10.0);
            }
            pos = Math.max(0, Math.min(pos, 1));
            switch (pickIdx) {
                case 0:
                    this.valH = 1f-(float)pos;
                    break;
                case 1:
                    this.valS = (float)pos;
                    break;
                case 2:
                    this.valL = (float)pos;
                    break;
            }
        }
        colorPick[0].posX =(int) ((xRight-extendSize/2)+(1-valH)*(wRight+(extendSize)-bWidth));
        colorPick[1].posX =(int) ((xRight-extendSize/2)+(valS)*(wRight+(extendSize)-bWidth));
        colorPick[2].posX =(int) ((xRight-extendSize/2)+(valL)*(wRight+(extendSize)-bWidth));
        for (int a = 0; a < 3; a++) {

            BoxGUI.setColorwheel("colorwheel", 1+a);
            BoxGUI.setHSL("valueH", valH, valS, valL);
            int m = this.posY+btnStep*a+2;
            renderRoundedBoxShadow(xRight, m, 3, wRight, btnWH-4, -1, 1, true);
        }
        BoxGUI.setColorwheel("colorwheel", 4);
        int rgb = Color.HSLtoRGB(1-valH, valS, valL);

        renderRoundedBoxShadow(this.posX, this.posY+1, 3, boxW, boxW, rgb, 1, true);
        BoxGUI.setColorwheel("colorwheel", 0);
        if (this.rgb != rgb) {
            this.rgb = rgb;
            onColorChange(rgb);
        }

        Engine.pxStack.push(0, 0, 4);
        FontRenderer f = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
        f.drawString(String.format("%08X", rgb), this.posX, this.posY+f.getLineHeight(), 0, true, 1.0f);
        Engine.pxStack.pop();
        
    }

    public abstract void onColorChange(int rgb2);
    
    @Override
    public void initGui(boolean first) {
        colorPick = new Button[3];
        int wRight = this.width-boxW-spacing;
        int xRight = this.posX+boxW+spacing;
        for (int i = 0; i < 3; i++) {
            colorPick[i] = new Button(-4, "");
            colorPick[i].parent = this.parent;
            colorPick[i].setPos(xRight, this.posY+i*btnStep);
            colorPick[i].setSize(btnWH, btnWH);
            colorPick[i].extendx=0;
            colorPick[i].extendy=0;
            colorPick[i].shadowSigma=0;
            colorPick[i].alpha*=0.5f;
            colorPick[i].alpha2*=0.5f;
            this.parent.add(colorPick[i]);
        }
    }
    public int getPickerHeight() {
        return Math.max(3*btnStep, this.height);
    }

    @Override
    public boolean hasElement(AbstractUI element) {
        for (int i = 0; i < 3; i++) {
            if (colorPick[i] == element) {
                return true;
            }
        }
        return false;
    }
    
}