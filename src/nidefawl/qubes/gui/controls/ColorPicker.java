package nidefawl.qubes.gui.controls;


import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Color;

public abstract class ColorPicker extends AbstractUI {
    private Button[] colorPick;
    float valH=0;
    float valS=1;
    float valL=0.5f;
    private int rgb;
    public ColorPicker(Gui parent) {
        this.parent = parent;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        final int extendSize = 32;
        int pickIdx = -1;
        for (int i = 0; i < 3; i++) {
            if (colorPick[i] == selectedButton) {
                pickIdx = i; break;
            }
        }
        int boxW = 40*3-10;
        int wRight = this.width-boxW-20;
        int xRight = this.posX+boxW+20;
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
            int m = this.posY+40*a+2;
            renderRoundedBoxShadow(xRight, m, 3, wRight, 28, -1, 1, true);
        }
        BoxGUI.setColorwheel("colorwheel", 4);
        int rgb = Color.HSBtoRGB(1f-valH, valS*(1-(Math.max(0, (valL-0.5f)*2))), Math.min(1, valL*2));
        
        renderRoundedBoxShadow(this.posX, this.posY+1, 3, boxW, boxW, rgb, 1, true);
        BoxGUI.setColorwheel("colorwheel", 0);
        if (this.rgb != rgb) {
            this.rgb = rgb;
            onColorChange(rgb);
        }
        
    }

    public abstract void onColorChange(int rgb2);
    
    @Override
    public void initGui(boolean first) {
        final int extendSize = 32;
        colorPick = new Button[3];
        int boxW = 40*3-10;
        int wRight = this.width-boxW-20;
        int xRight = this.posX+boxW+20;
        for (int i = 0; i < 3; i++) {
            colorPick[i] = new Button(-4, "");
            colorPick[i].parent = this.parent;
            colorPick[i].setPos(xRight, this.posY+i*40);
            colorPick[i].setSize(32, 32);
            colorPick[i].extendx=0;
            colorPick[i].extendy=0;
            colorPick[i].shadowSigma=0;
            colorPick[i].alpha*=0.5f;
            colorPick[i].alpha2*=0.5f;
            this.parent.add(colorPick[i]);
        }
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