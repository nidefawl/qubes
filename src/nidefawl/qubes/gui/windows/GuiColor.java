package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.biome.BiomeMeadow;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.controls.ColorPicker;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.biomes.HexBiome;

public class GuiColor extends GuiWindow {
    
    private ColorPicker colorPick;

    public GuiColor() {
    }
    public String getTitle() {
        return "Color";
    }
    @Override
    public void initGui(boolean first) {
        this.clearElements();
        this.colorPick = new ColorPicker(this) {
            @Override
            public void onColorChange(int rgb2) {
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
                        else {
                            Biome.MEADOW_GREEN
                            .setColor(BiomeColor.GRASS, 0x4f923b)
                            .setColor(BiomeColor.LEAVES, 0x4A7818)
                            .setColor(BiomeColor.FOLIAGE, 0x408A10)
                            .setColor(BiomeColor.FOLIAGE2, 0x64B051)
                            .setColor(BiomeColor.GRASS, rgb2);
                            Engine.regionRenderer.reRender();
                        }
                    }
                }
            }
        };
        this.colorPick.setPos(15, 20+titleBarHeight);
        this.colorPick.setSize(360, 130);
        this.colorPick.initGui(first);

        int bw = 60;
//        if (this.bounds != null) {
//            setPos(this.bounds[0], this.bounds[1]);
//            setSize(this.bounds[2], this.bounds[3]);
//        } else {
            int width = 390;
            int height = titleBarHeight+160;
            int xPos = (Game.displayWidth-width)/2;
            int yPos = (Game.displayHeight-height)/2;
            setPos(xPos, yPos);
            setSize(width, height);
//        }
    }
    protected boolean canResize() {
        return false;
    }

    public void render(float fTime, double mX, double mY) {
        Engine.pxStack.push(posX, posY, 4);
        this.colorPick.render(fTime, mX-posX, mY-posY);
        Engine.pxStack.pop();
        Engine.pxStack.push(0,0, 8);
        super.renderButtons(fTime, mX, mY);
        Engine.pxStack.pop();
    }
    public boolean onGuiClicked(AbstractUI element) {
//        element.posX+=30;
//        if (element.posX+element.width>this.width) {
//            element.posX = 0;
//        }
        if (this.colorPick.hasElement(element)) {
            return true;
        }
        return super.onGuiClicked(element);
    }

}
