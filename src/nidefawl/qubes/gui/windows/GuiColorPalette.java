package nidefawl.qubes.gui.windows;

import java.util.ArrayList;
import java.util.Map.Entry;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.controls.ColorPicker;
import nidefawl.qubes.util.ColorPalette;
import nidefawl.qubes.world.World;

public class GuiColorPalette extends GuiWindow {
    static final class ColorPaletteEntry {
        public ColorPaletteEntry(String key, ColorPicker colorPick) {
            this.key = key;
            this.colorPick = colorPick;
        }
        String key;
        ColorPicker colorPick;
    }
    ColorPalette palette = null;
    private final ArrayList<ColorPaletteEntry> pickerList;

    public GuiColorPalette(ColorPalette palette) {
        this.pickerList = new ArrayList<>();
        this.palette = palette;
    }
    public String getTitle() {
        return "Color Palette";
    }
    @Override
    public void initGui(boolean first) {
        this.clearElements();
        pickerList.clear();
        int yStart = 20+titleBarHeight;
        int yPosElement = yStart;
        int pickerHeight = 90;
        int xSpacing = 15;
        int totalW = 1280;
        int numCols = 8;
        int colW = (totalW-(xSpacing*(numCols+1)))/numCols;
        int nEntry = 0;
        for (Entry<String, Integer> entry : palette.map.entrySet()) {
            final String entryKey = entry.getKey();
            final int curRGB = palette.getOrSetColor(entryKey, -1);
            ColorPicker colorPick = new ColorPicker(this) {
                @Override
                public void onColorChange(int rgb2) {
                    palette.set(entryKey, rgb2);
                    if (Game.instance != null) {
                        World w = Game.instance.getWorld();
                        PlayerSelf p = Game.instance.getPlayer();
                        if (w != null && p != null) {
                            Block.leaves.updateColors();
                            Engine.regionRenderer.reRender();
                        }
                    }
                }
            };
            int xPos = xSpacing + (nEntry%numCols)*(colW+xSpacing);
            colorPick.setPos(xPos, yPosElement);
            colorPick.setSize(colW, pickerHeight);
            colorPick.setRgb(curRGB);
            colorPick.initGui(first);
            if ((nEntry)%numCols==numCols-1) {
                yPosElement += pickerHeight;
            }
            ColorPaletteEntry colorPickEntry = new ColorPaletteEntry(entryKey, colorPick);
            pickerList.add(colorPickEntry);
            nEntry++;
        }

        int bw = 60;
//        if (this.bounds != null) {
//            setPos(this.bounds[0], this.bounds[1]);
//            setSize(this.bounds[2], this.bounds[3]);
//        } else {
            int width = totalW;
            int height = yPosElement;
            int xPos = (Engine.getGuiWidth()-width)/2;
            int yPos = (Engine.getGuiHeight()-height)/2;
            setPos(xPos, yPos);
            setSize(width, height);
//        }
    }
    protected boolean canResize() {
        return false;
    }

    public void render(float fTime, double mX, double mY) {
        int i = 0;
        for (ColorPaletteEntry colorPickEntry : pickerList) {
            Engine.pxStack.push(posX, posY, 4);
            colorPickEntry.colorPick.render(fTime, mX-posX, mY-posY);
            FontRenderer f = FontRenderer.get(0, 12, 1);
            f.drawString(colorPickEntry.key, colorPickEntry.colorPick.posX, colorPickEntry.colorPick.posY, -1, true, 1.0f);
            Engine.pxStack.pop();
        }
        Engine.pxStack.push(0,0, 8);
        super.renderButtons(fTime, mX, mY);
        Engine.pxStack.pop();
    }
    public boolean onGuiClicked(AbstractUI element) {
//        element.posX+=30;
//        if (element.posX+element.width>this.width) {
//            element.posX = 0;
//        }
        for (ColorPaletteEntry colorPickEntry : pickerList) {
            if (colorPickEntry.colorPick.hasElement(element)) {
                return true;
            }
        }
        return super.onGuiClicked(element);
    }

}
