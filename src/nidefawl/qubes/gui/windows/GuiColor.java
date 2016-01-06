package nidefawl.qubes.gui.windows;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.controls.ColorPicker;

public class GuiColor extends GuiWindow {
    
    private ColorPicker colorPick;

    public GuiColor() {
    }
    public String getTitle() {
        return "Color";
    }
    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        this.colorPick = new ColorPicker(this);
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

    public void update() {
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
