package nidefawl.qubes.gui.controls;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ComboBox extends AbstractUI implements Renderable {
    private FontRenderer font;
    
    public int           textColorDisabled = 0xa0a0a0;
    public int           textColorHover    = 0xffffa0;
    public int           textColor         = 0xe0e0e0;
    public float         r                 = 1.0f;
    public float         g                 = 1.0f;
    public float         b                 = 1.0f;
    public float         a                 = 1.0f;
    public Object        value             = null;
    public int           stringWidth;
    public boolean       isOpen            = false;
    public int           sel;
    public int           id;
    private final String string;
    public boolean       enabled           = true;
    public boolean       drawTitle         = true;
    public boolean       titleLeft         = false;
    public boolean canexpandHorizontally = false;
    public int maxWidthClosed = -1;
    
    public int titleWidth = 32;
    private ComboBoxList comboBoxList;
    static int scrollbarwidth = 12;
    
    public ComboBox(int id, String text) {
        this.id = id;
        this.string = text;
        this.width = this.height = 18;
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.stringWidth = this.font.getStringWidth(this.string);
    }
    public void setValue(Object obj) {
        this.value = obj;
    }
    public ComboBox(final int id, final String s, boolean stringLeft) {
        this.string = s;
        this.id = id;
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.height = this.font.getLineHeight()+4;
        this.stringWidth = this.font.getStringWidth(s);
        titleWidth=Math.max(stringWidth+6, titleWidth);
        this.titleLeft = stringLeft;
    }

    @Override
    public void initGui(boolean first) {

    }


    public static interface CallBack {
        void call(ComboBoxList combo, int id);
    }

    public static class ComboBoxList extends AbstractUIOverlay {
        PopupHolder            parentScreen;
        private final CallBack callBack;
        public ComboBox        box;
        private final Object[] values;
        private int            size = 4;
        private int            heightPerEntry;
        private boolean        showScrollBar;
        private int            scrollOffset = 0;
        public int             showMax      = 6;
        boolean                isScrolling  = false;
        private int            scrollBeginY;

        public Object getValue(int id) {
            return values != null && id >= 0 && id < values.length ? values[id] : null;
        }
        public ComboBoxList setSize(int i) {
            this.showMax = i;
            return this;
        }

        public ComboBoxList(final CallBack popupCallBack, final PopupHolder parentScreen, final ComboBox box, final Object[] values) {
            this.parentScreen = parentScreen;
            this.callBack = popupCallBack;
            this.box = box;
            this.box.setWatchPopup(this);
            this.values = values == null ? new Object[0] : values;
        }
        
        @Override
        public void initGui(boolean first) {
            super.initGui(first);
            setFocus();
        }

        public boolean onKeyPress(int i, int c, int action, int mods) {
            if (c == 0) {
                int iScroll = i == GLFW.GLFW_KEY_UP ? -1 : i == GLFW.GLFW_KEY_DOWN ? 1 : 0;
                if (iScroll != 0) {
                    this.box.sel += iScroll;
                    if(this.box.sel < 0)
                        this.box.sel = this.values.length -1;
                    if(this.box.sel >= this.values.length)
                        this.box.sel = 0;
                    if (scrollOffset> this.box.sel) {
                        scrollOffset = this.box.sel;
                    }
                    if (scrollOffset+showMax <= this.box.sel) {
                        scrollOffset = this.box.sel-showMax+1;
                    }
                    return true;
                }
            }
            if (i == GLFW.GLFW_KEY_ESCAPE) {
                this.parentScreen.setPopup(null);
                this.box.isOpen = false;
                this.callBack.call(this, -1);
                return true;
            }
            if (i == GLFW.GLFW_KEY_SPACE || i == GLFW.GLFW_KEY_ENTER || i == GLFW.GLFW_KEY_KP_ENTER) {
                this.parentScreen.setPopup(null);
                this.box.isOpen = false;
                this.callBack.call(this, this.box.sel);
                return true;
            }
            return false;
        }

        /* (non-Javadoc)
         * @see nidefawl.qubes.gui.AbstractUI#mouseOver(double, double)
         */
        @Override
        public boolean mouseOver(double i, double j) {
            int rowWidth = showScrollBar ? this.width - scrollbarwidth : this.width;
            isScrolling = false;
            this.scrollBeginY = 0;
            if (i > this.posX && i < this.posX + rowWidth && j > this.posY && j < this.posY + this.height) {
                return true;
            }
            if (showScrollBar && i > this.posX + rowWidth && i < this.posX + this.width && j > this.posY && j < this.posY + this.height) {
                isScrolling = true;
                return true;
            }
            if (this.box.isOpen && !this.box.mouseOver(i, j)) {
                this.parentScreen.setPopup(null);
                this.box.isOpen = false;
                this.callBack.call(this, -1);
                return true;
            }
            return super.mouseOver(i, j);
        }
        /* (non-Javadoc)
         * @see nidefawl.qubes.gui.AbstractUI#handleMouseDown(nidefawl.qubes.gui.Gui, int)
         */
        @Override
        public boolean handleMouseDown(Gui gui, int action) {
            if (isScrolling) return true;
            this.parentScreen.setPopup(null);
            this.box.isOpen = false;
            this.callBack.call(this, this.box.sel);
            return false;
        }
        
        /* (non-Javadoc)
         * @see nidefawl.qubes.gui.controls.AbstractUIOverlay#render(float, double, double)
         */
        @Override
        public void render(float f, double i, double j) {

            int values = this.values.length;
            int scrollbarwidth = 12;
            if (values > showMax) {
                values = showMax;
                showScrollBar = true;
            } else {
                showScrollBar = false;
                isScrolling = false;
                scrollbarwidth = 0;
            }
            if (this.values.length > 0) {
                this.heightPerEntry = box.font.getLineHeight() - 1;
                this.height = values * this.heightPerEntry;
                this.width = this.box.width;
                this.posY = this.box.posY + this.box.height;
                this.posX = this.box.posX;
                if (this.posY + this.height > Game.displayHeight && this.box.posY - this.height > 0) {
                    this.posY = this.box.posY - this.height;
                }
                float len = ((float) values / (float) this.values.length);
                boolean buttonDown = Mouse.isButtonDown(0);
                int rowWidth = this.width - scrollbarwidth;
                if (i > this.posX && i < this.posX + rowWidth && j > this.posY && j < this.posY + this.height) {
                    this.box.sel = -(((this.posY - (int)j) / this.heightPerEntry))+this.scrollOffset;
                }
                double mx = Mouse.scrollDY;
                Mouse.scrollDY = 0;
                if (showScrollBar) {
                    if (isScrolling && !buttonDown) {
                        isScrolling = false;
                    }
                    if (isScrolling) {
                        float scrolled = (float)(this.posY - j) / ((float)heightPerEntry*len);
                        scrollOffset = this.scrollBeginY-(int)scrolled;
                    }
                    if (mx > 0) {
                        scrollOffset--;
                    }
                    else if (mx < 0) {
                        scrollOffset++;
                    }
                }
                if (scrollOffset < 0)
                    scrollOffset = 0;
                if (scrollOffset+values >= this.values.length) {
                    scrollOffset = (this.values.length) - values;
                }
                final Tess tessellator = Tess.instance;
                Shaders.colored.enable();
//                GL11.glEnable(3042 /* GL_BLEND */);
//                GL11.glBlendFunc(770, 771);
//                GL11.glDisable(3553 /* GL_TEXTURE_2D */);
//                OpenGlHelper.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
//
//                tessellator.startDrawingQuads();
                tessellator.add(this.posX + this.width, this.posY, 0.0f);
                tessellator.add(this.posX, this.posY, 0.0f);
                tessellator.add(this.posX, this.posY + this.height, 0.0f);
                tessellator.add(this.posX + this.width, this.posY + this.height, 0.0f);
                tessellator.drawQuads();
                GL11.glEnable(3553 /* GL_TEXTURE_2D */);
                for (int c = 0; c < values ; c++) {
                    int i1 = 0xFFFFFF;
                    if (c+scrollOffset == this.box.sel) {
                        i1 = this.box.textColorHover;
//                        GL11.glEnable(3042 /* GL_BLEND */);
//                        GL11.glDisable(3553 /* GL_TEXTURE_2D */);
                        Shaders.colored.enable();
                        GL11.glBlendFunc(770, 771);
//                        OpenGlHelper.glColor4f(this.box.r, this.box.g, this.box.b, 0.8F);
//                        tessellator.startDrawingQuads();
                        tessellator.add(this.posX + rowWidth, this.posY + (c * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX, this.posY + (c * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX, this.posY + ((c + 1) * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX + rowWidth, this.posY + ((c + 1) * this.heightPerEntry), 0.0f);
                        tessellator.drawQuads();
                        GL11.glEnable(3553 /* GL_TEXTURE_2D */);
                    }
                    String entry = String.valueOf(this.values[c+scrollOffset]);
                    int w = box.font.getStringWidth(entry);
                    box.font.maxWidth = rowWidth - 8;
                    final int yStringPos = this.posY - 1 + box.font.getLineHeight() + (c * this.heightPerEntry);
                    Shaders.textured.enable();
                    box.font.drawString(entry, this.posX + this.size, yStringPos, i1, true, 1.0f);

                    if (box.canexpandHorizontally) {
                        int wRow = rowWidth-size*2;
                        if (w>=wRow) {
                            int w2 = size*2+w+scrollbarwidth;
                            if (w2 > box.width)
                            box.width = w2;
                        }
                    }
                    box.font.maxWidth = -1;
                }
                GL11.glEnable(3042 /* GL_BLEND */);
                GL11.glDisable(3553 /* GL_TEXTURE_2D */);
                GL11.glBlendFunc(770, 771);
//                OpenGlHelper.glColor4f(1.0F, 1.0F, 1.0F, 0.3F);
                GL11.glLineWidth(2.0F);
                Shaders.colored.enable();
                int yFrame = this.posY;
                int bottom = yFrame +(values * this.heightPerEntry);
//                tessellator.startDrawing(GL11.GL_LINE_STRIP);
                tessellator.add(this.posX + this.width, yFrame, 0.0f);
                tessellator.add(this.posX, yFrame, 0.0f);
                tessellator.add(this.posX, bottom, 0.0f);
                tessellator.add(this.posX + this.width, bottom, 0.0f);
                tessellator.add(this.posX + this.width, yFrame, 0.0f);
                tessellator.draw(GL11.GL_LINE_STRIP);
                if (showScrollBar && (bottom - this.posY)>0) {
                    int heightH = (bottom - this.posY);
                    int scrollBarX = this.posX+this.width-(scrollbarwidth-2);
                    int scrollBarRight = scrollBarX+(scrollbarwidth-3);
                    int posY= this.posY+1;
                    float scrolled = ((float) (scrollOffset) / (float) (this.values.length-values));
                    
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(770, 771);
//                    GL11.glShadeModel(GL11.GL_SMOOTH);
                    int totalContentHeight = heightPerEntry*this.values.length;
                    int scrollerHeight = ((bottom - posY) * (bottom - posY)) / totalContentHeight;
                    if (scrollerHeight < 32) {
                        scrollerHeight = 32;
                    }
                    if (scrollerHeight > bottom - posY - 8) {
                        scrollerHeight = bottom - posY - 8;
                    }
                    int hScrollRoom = bottom - posY - scrollerHeight - 1;
                    int scrollerTop = posY;
                    scrollerTop+=scrolled*hScrollRoom;
                    if (scrollerTop < posY) {
                        scrollerTop = posY;
                    }

//                    tessellator.startDrawingQuads();
                    tessellator.setColorF(0x898989, 255);
                    tessellator.add(scrollBarX, bottom, 0.0f, 0.0f, 1.0f);
                    tessellator.add(scrollBarRight, bottom, 0.0f, 1.0f, 1.0f);
                    tessellator.add(scrollBarRight, posY, 0.0f, 1.0f, 0.0f);
                    tessellator.add(scrollBarX, posY, 0.0f, 0.0f, 0.0f);
                    tessellator.drawQuads();
//                    tessellator.startDrawingQuads();
                    tessellator.setColorF(0xc0c0c0, 255);
                    tessellator.add(scrollBarX, scrollerTop + scrollerHeight, 0.0f, 0.0f, 1.0f);
                    tessellator.add(scrollBarRight, scrollerTop + scrollerHeight, 0.0f, 1.0f, 1.0f);
                    tessellator.add(scrollBarRight, scrollerTop, 0.0f, 1.0f, 0.0f);
                    tessellator.add(scrollBarX, scrollerTop, 0.0f, 0.0f, 0.0f);
                    tessellator.drawQuads();

                    float fa = 1F;
//                    OpenGlHelper.glColor3f(fa, fa, fa);
                    GL11.glLineWidth(1.0F);
                    
//                    GL11.glBegin(GL11.GL_LINE_STRIP);
                    tessellator.add(scrollBarRight - 2, scrollerTop + 2);
                    tessellator.add(scrollBarX + 1, scrollerTop + 2);
                    tessellator.add(scrollBarX + 1, scrollerTop + scrollerHeight);
                    tessellator.draw(GL11.GL_LINE_STRIP);
//                    GL11.glEnd();
                    fa = 0.2F;
//                    OpenGlHelper.glColor3f(fa, fa, fa);
                    GL11.glLineWidth(2.0F);
//                    GL11.glBegin(GL11.GL_LINE_STRIP);
                    tessellator.setColorRGBAF(fa, fa, fa, 1.0f);
                    tessellator.add(scrollBarRight, scrollerTop);
                    tessellator.add(scrollBarRight, scrollerTop + scrollerHeight);
                    tessellator.add(scrollBarX, scrollerTop + scrollerHeight);
                    tessellator.draw(GL11.GL_LINE_STRIP);
//                    GL11.glEnd();
//                    GL11.glShadeModel(GL11.GL_FLAT);
                }
                GL11.glEnable(3553 /* GL_TEXTURE_2D */);
                GL11.glDisable(3042 /* GL_BLEND */);
                
            } else {
                this.box.sel = -1;
            }
        }
    }

    public void setWatchPopup(ComboBoxList comboBoxList) {
        this.comboBoxList = comboBoxList;
    }


    public void setFont(FontRenderer font) {
        this.font = font;
        this.height = this.font.getLineHeight()+4;
        this.stringWidth = this.font.getStringWidth(this.string);
    }

    protected int getHoverState(final boolean flag) {
        byte byte0 = 1;
        if (!this.enabled) {
            byte0 = 0;
        } else if (flag) {
            byte0 = 2;
        }
        return byte0;
    }

    @Override
    public void render(float f, double i, double j) {
        if (isOpen) {
            if (this.comboBoxList == null || this.comboBoxList.parentScreen == null)
                isOpen = false;
            else if (this.comboBoxList.parentScreen.getPopup() != this.comboBoxList)
                isOpen = false;
            if (!isOpen) {
                System.out.println("popup was closed");
                this.comboBoxList = null;
            }
        }
        GL11.glDepthMask(false);
        
        this.hovered = this.mouseOver(i, j);
        
        

        Shaders.colored.enable();
        renderBox();
        Shaders.colored.enable();
        final Tess tessellator = Tess.instance;
        tessellator.setColorF(0xeaeaea, hovered ? 1 : 0.7F);
        tessellator.add(this.posX + this.width - 1, this.posY + 2, 0.0f);
        tessellator.add(this.posX + width - height + 1, this.posY + 2, 0.0f);
        tessellator.add(this.posX + width - height + 1, this.posY + this.height - 1, 0.0f);
        tessellator.add(this.posX + this.width - 1, this.posY + this.height - 1, 1.0f);
        tessellator.drawQuads();

        tessellator.setColorF(0, 0.7F);
        int inset = height / 6;
        if (inset < 1)
            inset = 1;
        tessellator.add(this.posX + this.width - height + inset, this.posY + inset * 2 + 1, 0.0f);
        tessellator.add(this.posX + this.width - height + inset + (height - inset * 2) / 2, this.posY + this.height - inset * 2 + 2, 0.0f);
        tessellator.add(this.posX + this.width - inset, this.posY + inset * 2 + 1, 0.0f);
        tessellator.draw(GL11.GL_POLYGON);

        Shaders.textured.enable();
        int colorDisabled = 0xCCCCCC;
        if (inset < 4) inset = 4;
        this.font.maxWidth = this.width - height - inset;
        String val = String.valueOf(this.value);
        this.font.drawString(val, this.posX + 4, this.posY + (this.height+this.font.getLineHeight())/2, this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);

        int w = this.font.getStringWidth(val);
        if (canexpandHorizontally && !isOpen) {
            this.width = height+inset+w+4;
        }
        this.font.maxWidth = -1;
        if (this.drawTitle) {
            if (titleLeft) {
                this.font.drawString(this.string, this.posX - titleWidth, this.posY + (this.height+this.font.getLineHeight())/2, this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);

            } else {
                this.font.drawString(this.string, this.posX + this.width + 5, this.posY + (this.height+this.font.getLineHeight())/2, this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);
            }

        }
        GL11.glDepthMask(true);
    }
    /**
     * @param guiSettings
     * @return
     */
    public boolean onClick(PopupHolder parent) {
        if (this.isOpen) {
            this.isOpen = false;
            if (parent.getPopup() != null) {
                parent.setPopup(null);
                return false;
            }
        }
        if (!this.isOpen) {
            this.isOpen = true;
            return true;
        }
        return false;
    }

//    public boolean mouseOver(final int i, final int j) {
//        return this.enabled && i > this.posX && i < this.posX + this.width && j > this.posY && j < this.posY + this.height;
//    }

//
//    @Override
//    public int getMinWidth() {
//        return this.width;
//    }
//
//    @Override
//    public int getMinHeight() {
//        return this.font.getLineHeight() - 2;
//    }

}