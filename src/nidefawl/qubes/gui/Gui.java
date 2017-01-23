package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.PopupHolder;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;

public abstract class Gui extends AbstractUI implements PopupHolder {
    public ArrayList<AbstractUI> buttons   = new ArrayList<>();
    public ArrayList<AbstractUI> prebackground   = new ArrayList<>();
    public boolean    firstOpen = true;
    public boolean isFullscreen = false;
    public AbstractUI popup;
    public static final int slotW = 48;
    public static final int slotBDist = 2;
    public static final int titleBarHeight = 26;
    public static final int titleBarOffset = titleBarHeight+20;
    public static boolean RENDER_BACKGROUNDS = true;
    public static int FONT_SIZE_WINDOW_TITLE = 22;
    public static int FONT_SIZE_BUTTON = 18;
    final public FontRenderer titleFont;
    final public FontRenderer font;
    
    public Gui() {
        this.titleFont = FontRenderer.get(0, Gui.FONT_SIZE_WINDOW_TITLE, 0);
        this.font = FontRenderer.get(0, 18, 0);
    }

    @Override
    public AbstractUI getPopup() {
        return this.popup;
    }
    public boolean hasElement(AbstractUI element) {
        return buttons.contains(element) /*|| this.popup == element */;
    }
    public void add(AbstractUI element) {
        this.buttons.add(element);
        element.parent = this;
        sortElements();
    }
    public void addBackground(AbstractUI element) {
        this.buttons.add(element);
        this.prebackground.add(element);
        element.parent = this;
        sortElements();
    }
    public AbstractUI getElement(int i) {
        return i>=0&&i<this.buttons.size()?this.buttons.get(i):null;
    }
    public void clearElements() {
        this.buttons.clear();
        this.prebackground.clear();
    }
    public void sortElements() {
        Comparator<AbstractUI> comparator = new Comparator<AbstractUI>() {
            @Override
            public int compare(AbstractUI o1, AbstractUI o2) {
                int n = Integer.compare(o1.zIndex,  o2.zIndex);
                if (n != 0)
                    return n;
                return Integer.compare(o1.id, o2.id);
            }
        };
        Collections.sort(this.prebackground, comparator);
        Collections.sort(this.buttons, comparator);
    }

    public void remove(AbstractUI element) {
        boolean b = this.buttons.remove(element);
        if (b) {
            element.parent=null;
        }
    }
    @Override
    public void setPopup(AbstractUI popup) {
        if (selectedButton == popup) {
            selectedButton = null;
        }
        this.popup = popup;
        if (popup != null) {
            popup.parent = this;
            popup.initGui(false);   
        }
    }

    public void renderBackgroundElements(float fTime, double mX, double mY) {
        if (prebackground.isEmpty())
            return;
        Engine.pxStack.push(this.posX, this.posY, 0);
        for (int i = 0; i < this.prebackground.size(); i++) {
            this.prebackground.get(i).render(fTime, mX-this.posX, mY-this.posY);
        }
        Engine.pxStack.pop();
    }
    public void renderButtons(float fTime, double mX, double mY) {
        Engine.pxStack.push(this.posX, this.posY, 2);
        double mx = mX;
        double my = mY;
        if (this.popup != null) {
            mx -= (this.posX + this.popup.posX);
            my -= (this.posY + this.popup.posY);
            if (mx > 0 && mx < this.popup.width && my > 0 && my <= this.popup.height) {
                mX-=1000;
                mY-=1000;
            }
            mx += (this.posX + this.popup.posX);
            my += (this.posY + this.popup.posY);
        }
        int lastZ=Integer.MIN_VALUE;
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI btn = this.buttons.get(i);
            if (this.prebackground.contains(btn)) {
                continue;
            }
            if (btn.zIndex!=lastZ) {
                lastZ = btn.zIndex;
                Engine.pxStack.translate(0, 0, 8);
            }
            btn.render(fTime, mX-this.posX, mY-this.posY);
        }
        Engine.pxStack.pop();
        if (this.popup != null) {
            Engine.pxStack.push(this.posX, this.posY, 200);
            this.popup.render(fTime, mx-this.posX, my-this.posY);
            Engine.pxStack.pop();
        }
    }
    @Override
    public void update() {
        super.update();
        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).update();
        }
    }

    public void onClose() {
    }

    public boolean onMouseClick(int button, int action) {
        double mx=mouseGetX()-mouseOffsetX();
        double my=mouseGetY()-mouseOffsetY();
        if (action == GLFW.GLFW_PRESS) {
            for (int i = 0; i < this.buttons.size(); i++) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.draw) {
                    b.focused = false;
                }
            }
        }
        if (selectedButton != null && action == 0) {
            if (selectedButton.parent != null && selectedButton.parent != this) {
                return false;
            }
            if (selectedButton.enabled && selectedButton.mouseOver(mx, my)) {
                selectedButton.handleMouseUp(this, action);
            }
            selectedButton = null;
            return true;
        } else if (selectedButton == null && action == 1){
            if (this.popup != null) {
                if (this.popup.mouseOver(mx, my)) {
                    if (this.popup != null && !this.popup.handleMouseDown(this, action)) {
                        selectedButton = this.popup;
                    }
                    return true;
                }
            }
            for (int i = this.buttons.size()-1; i >= 0; i--) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.mouseOver(mx, my)) {
                    if (!b.handleMouseDown(this, action)) {
                        selectedButton = b;    
                    }
                    return true;
                }
            }
        }
        selectedButton = null;
        return false;
    }

    public double mouseOffsetY() {
        return this.posY+(this.parent!=null?this.parent.getWindowPosY():0);
    }

    public double mouseOffsetX() {
        return this.posX+(this.parent!=null?this.parent.getWindowPosX():0);
    }

    public boolean onGuiClicked(AbstractUI element) {
        return false;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            close();
            return true;
        }
        if (this.popup != null) {
            if (this.popup.onKeyPress(key, scancode, action, mods)) {
                return true;
            }
        }
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI b = this.buttons.get(i);
            if (b.enabled) {
                if (b.onKeyPress(key, scancode, action, mods)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void close() {
        GameBase.baseInstance.showGUI((Gui) this.parent);
    }

    public boolean onTextInput(int codepoint) {
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI b = this.buttons.get(i);
            if (b.enabled) {
                if (b.onTextInput(codepoint)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean requiresTextInput() {
        return false;
    }
    
    

    public void renderFrame(float fTime, double mX, double mY) {
        float c = 0.1f;
        float ac = 0.3f;
        if (Game.instance != null&&Game.instance.getWorld() == null) {
            GL11.glClearColor(c,c,c, ac);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }
        if (this.isFullscreen) {
            renderBackgroundElements(fTime, mX, mY);
            if (Game.instance.getWorld() != null) {
                Shaders.colored.enable();
                Tess.instance.setColorRGBAF(c, c, c, ac);
                Tess.instance.add(posX, posY+height);
                Tess.instance.add(posX+width, posY+height);
                Tess.instance.add(posX+width, posY);
                Tess.instance.add(posX, posY);
                Tess.instance.drawQuads();
            }
        } else {
            renderBackgroundElements(fTime, mX, mY);
            this.posY+=5;
            this.round = 1;
            this.extendx = 0;
            this.round = 2;
            Shaders.gui.enable();
            this.shadowSigma = 3f;
            this.round = 3;
            int bw = 3;
            int out=2;
            float alpha = 0.8f;
            int color = -1;//this.hasFocus() ? -1 : 0x9a9a9a;
            if (this.hasFocus()) {
                int n = 4;
                Shaders.gui.enable();
                Shaders.gui.setProgramUniform1f("zpos", -18);
                Shaders.gui.setProgramUniform4f("box", posX-n, posY-n, posX+width+n, posY+height+n);
//              Shaders.gui.setProgramUniform4f("color", 1-r, 1-g, 1-b, alpha);
//              float blink = GameMath.sin((((Game.ticksran+fTime)/20f)%20.0f)*GameMath.PI*2f)*0.5f+0.5f;
//              float c = 0.8f*blink;
//                Shaders.gui.setProgramUniform1f("sigma", 4+c*5);
//            Shaders.gui.setProgramUniform4f("color", c,c,c, c);
//            shadowSigma = c*10;
                Shaders.gui.setProgramUniform4f("color", c,c,c, ac);
                Shaders.gui.setProgramUniform1f("sigma", 2);
                Shaders.gui.setProgramUniform1f("corner", 8);
                Engine.drawQuad();
            }
            int z = -10;

            resetShape();
            alpha = 0.3f;
            Shaders.gui.setProgramUniform1f("fade", 0.1f);
            renderBox();
            Shaders.gui.setProgramUniform1f("fade", 0.3f);
            out=-30;
            alpha = 1;
            color = this.hasFocus() ? 0xdadada : 0xbababa;
//            color = 0xdadada;
            this.shadowSigma = 4f;
            int titleWidth = 220;
            int top = (posY-4);
            int left = posX+width/2-titleWidth/2;
            renderRoundedBoxShadow(left, top, 5, titleWidth, titleBarHeight, color, 1f, true);
            resetShape();
//            GL11.glDepthFunc(GL11.GL_EQUAL);
//            this.shadowSigma = 3f;
//            this.round = 32;
//            renderRoundedBoxShadow(posX-titleBarHeight, posY-out*2-1, 5, width/3+4, titleBarHeight+out*2, -1, 0.7f, true);
//            GL11.glDepthFunc(GL11.GL_LEQUAL);
            resetShape();
            if (!this.isFullscreen) {
                Shaders.textured.enable();
                Engine.pxStack.push(0, 0, 6);
                titleFont.drawString(getTitle(), posX+width/2, posY + titleFont.centerY(titleBarHeight-16), -1, true, 1f, 2);
                Engine.pxStack.pop();
            }
            this.posY-=5;
        }
    }
    
    public void renderBackground(float fTime, double mX, double mY, boolean b, float a) {
        if (RENDER_BACKGROUNDS) {
            renderFrame(fTime, mX, mY);   
        } else {
            renderBackgroundElements(fTime, mX, mY);
        }
    }

    protected String getTitle() {
        return "";
    }

    protected boolean hasFocus() {
        return focused;
    }
    protected void updateBounds() {
    }

    protected boolean canResize() {
        return false;
    }

    public boolean onWheelScroll(double xoffset, double yoffset) {
        return false;
    }
    public double mouseGetX() {
        return GuiContext.mouseX;
    }
    public double mouseGetY() {
        return GuiContext.mouseY;
    }
    public void render3D(float fTime, double mX, double mY) {
        this.render(fTime, mX, mY);
    }
    public boolean mouseOver(double mX, double mY) {
        boolean b = super.mouseOver(mX, mY);
        if (b)
            return true;

        if (this.popup != null) {
            mX -= (this.posX + this.popup.posX);
            mY -= (this.posY + this.popup.posY);
            if (mX > 0 && mX < this.popup.width && mY > 0 && mY <= this.popup.height) {
                return true;
            }
        }
        return false;
    }
}
