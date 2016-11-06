package nidefawl.qubes.gui.controls;


import nidefawl.qubes.Game;
import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.biomes.HexBiome;

public class ColorPicker extends AbstractUI {
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
        colorPick[0].posX =(int) ((xRight-extendSize/2)+(1-valH)*(wRight+(extendSize)-bWidth));;
        colorPick[1].posX =(int) ((xRight-extendSize/2)+(valS)*(wRight+(extendSize)-bWidth));;
        colorPick[2].posX =(int) ((xRight-extendSize/2)+(valL)*(wRight+(extendSize)-bWidth));;
        Shaders.gui.enable();
        for (int a = 0; a < 3; a++) {

            Shaders.gui.setProgramUniform1i("colorwheel", 1+a);
            Shaders.gui.setProgramUniform1f("valueH", valH);
            Shaders.gui.setProgramUniform1f("valueS", valS);
            Shaders.gui.setProgramUniform1f("valueL", valL);
            int m = this.posY+40*a+2;
            renderRoundedBoxShadow(xRight, m, 3, wRight, 28, -1, 1, true);
        }
        Shaders.gui.setProgramUniform1i("colorwheel", 4);
        int rgb = Color.HSBtoRGB(1f-valH, valS*(1-(Math.max(0, (valL-0.5f)*2))), Math.min(1, valL*2));
        
        renderRoundedBoxShadow(this.posX, this.posY+1, 3, boxW, boxW, rgb, 1, true);
        Shaders.gui.setProgramUniform1i("colorwheel", 0);
        if (this.rgb != rgb) {
            this.rgb = rgb;
            onColorChange(rgb);
        }
        
    }

    private void onColorChange(int rgb2) {
        System.out.println("color "+rgb2);
        if (Game.instance != null) {
            World w = Game.instance.getWorld();
            PlayerSelf p = Game.instance.getPlayer();
            if (w != null && p != null) {
                int x = GameMath.floor(p.pos.x);
                int z = GameMath.floor(p.pos.z);
                HexBiome hex = w.getHex(x, z);
                if (hex != null) {
                    hex.biome.colorGrass = rgb2;
                    hex.biome.colorFoliage = rgb2;
                    hex.biome.colorLeaves = rgb2;
                    float[] hsb = Color.RGBtoHSB((rgb2>>8)&0xFF, (rgb2>>8)&0xFF, rgb2&0xFF, null);
                    int rgb3 = Color.HSBtoRGB(hsb[0], hsb[1]*0.7f, hsb[2]*0.9f);
                    hex.biome.colorFoliage2 = rgb3;
                    
                    Engine.regionRenderer.reRender();
                }
            }
        }

    }

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