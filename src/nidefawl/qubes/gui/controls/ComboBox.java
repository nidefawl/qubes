package nidefawl.qubes.gui.controls;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.LineGUI;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;
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
    public float         stringWidth;
    public boolean       isOpen            = false;
    public int           sel;
    public int           id;
    private final String string;
    public boolean       drawTitle         = true;
    public boolean       titleLeft         = false;
    public boolean canexpandHorizontally = false;
    public int maxWidthClosed = -1;
    
    public int titleWidth = 32;
    private ComboBoxList comboBoxList;

    private Gui gui;
    static int scrollbarwidth = 12;
    
    public ComboBox(final Gui gui, int id, String text) {
        this.gui = gui;
        this.id = id;
        this.string = text;
        this.width = this.height = Gui.FONT_SIZE_BUTTON;
        this.font = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
        this.stringWidth = this.font.getStringWidth(this.string);
    }
    public void setValue(Object obj) {
        this.value = obj;
    }
    public ComboBox(final Gui gui, final int id, final String s, boolean stringLeft) {
        this.gui = gui;
        this.string = s;
        this.id = id;
        this.font = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
        this.height = GameMath.round(this.font.getLineHeight()+4);
        this.stringWidth = this.font.getStringWidth(s);
        titleWidth=GameMath.round(Math.max(stringWidth+6, titleWidth));
        this.titleLeft = stringLeft;
    }

    @Override
    public void initGui(boolean first) {

    }


    public static interface CallBack {
        void call(ComboBoxList combo, int id);
    }

    public static class ComboBoxList extends AbstractUI {
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
            extendx=1;
            extendy=2;
        }
        
        @Override
        public void initGui(boolean first) {
            setFocus();
        }

        public boolean onKeyPress(int key, int scancode, int action, int mods) {
            int iScroll = key == GLFW.GLFW_KEY_UP ? -1 : key == GLFW.GLFW_KEY_DOWN ? 1 : 0;
            if (iScroll != 0) {
                if (action != GLFW.GLFW_RELEASE) {
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
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (action != GLFW.GLFW_RELEASE) {
                this.parentScreen.setPopup(null);
                this.box.isOpen = false;
                this.callBack.call(this, -1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_SPACE || key== GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (action != GLFW.GLFW_RELEASE) {
                    this.parentScreen.setPopup(null);
                    this.box.isOpen = false;
                    this.callBack.call(this, this.box.sel);
                }
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
            if (!enabled) return true;
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
                this.heightPerEntry = GameMath.round(box.font.getLineHeight());
                this.height = values * this.heightPerEntry;
                this.width = this.box.width;
                this.posY = this.box.posY + this.box.height+extendy;
                this.posX = this.box.posX;
                if (this.posY + this.height > Engine.guiHeight && this.box.posY - this.height > 0) {
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
//                renderRoundedBoxShadow(this.posX, this.posY, 10, this.width, this.height, 0xababab, 1f, true);
                renderBox(true, true, color2, color3);
                Engine.setPipeStateColored2D();
                ITess tessellator = Engine.getTess();
                for (int c = 0; c < values ; c++) {
                    int i1 = 0xFFFFFF;
                    if (c+scrollOffset == this.box.sel) {
                        i1 = this.box.textColorHover;
                        Engine.setPipeStateColored2D();
                        tessellator.setColorF(0, 0.6f);
                        tessellator.add(this.posX + rowWidth, this.posY + (c * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX, this.posY + (c * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX, this.posY + ((c + 1) * this.heightPerEntry), 0.0f);
                        tessellator.add(this.posX + rowWidth, this.posY + ((c + 1) * this.heightPerEntry), 0.0f);
                        tessellator.drawQuads();
                    }
                    String entry = String.valueOf(this.values[c+scrollOffset]);
                    int w = GameMath.round(box.font.getStringWidth(entry));
                    box.font.maxWidth = rowWidth - 8;
                    float entryBoxBottom = this.posY-1+(heightPerEntry) + (c * this.heightPerEntry);
                    entryBoxBottom-=(heightPerEntry-2-box.font.getCharHeight())/2.0f;
                    final int yStringPos = GameMath.round(entryBoxBottom);
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
                    if (c+1<values) {
                        Engine.setPipeStateColored2D();
                        int y = this.posY + ((c + 1) * this.heightPerEntry);
                        LineGUI.INST.start(1F);
                        LineGUI.INST.add(this.posX + 4, y, 4, -1, 0.3f);
                        LineGUI.INST.add(this.posX + rowWidth - 4, y, 4, -1, 0.3f);
                        LineGUI.INST.drawLines();
                    }
                }
                int yFrame = this.posY;
                int bottom = yFrame +(values * this.heightPerEntry);
                if (showScrollBar && (bottom - this.posY)>0) {
                    int heightH = (bottom - this.posY);
                    int scrollBarX = this.posX+this.width-(scrollbarwidth-2);
                    int scrollBarRight = scrollBarX+(scrollbarwidth-3);
                    int posY= this.posY+1;
                    float scrolled = ((float) (scrollOffset) / (float) (this.values.length-values));

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
                    Engine.setPipeStateColored2D();

                    tessellator.setColorF(0xcecece, 255);
                    tessellator.add(scrollBarX, bottom, 0.0f);
                    tessellator.add(scrollBarRight, bottom, 0.0f);
                    tessellator.add(scrollBarRight, posY, 0.0f);
                    tessellator.add(scrollBarX, posY, 0.0f);
                    tessellator.drawQuads();

                    tessellator.setColorF(0xc0c0c0, 255);
                    tessellator.add(scrollBarX, scrollerTop + scrollerHeight, 0.0f);
                    tessellator.add(scrollBarRight, scrollerTop + scrollerHeight, 0.0f);
                    tessellator.add(scrollBarRight, scrollerTop, 0.0f);
                    tessellator.add(scrollBarX, scrollerTop, 0.0f);
                    tessellator.drawQuads();

                    LineGUI.INST.start(1f);
                    LineGUI.INST.add(scrollBarRight - 2, scrollerTop + 1, 0, -1, 0.5f);
                    LineGUI.INST.add(scrollBarX + 1, scrollerTop + 1, 0, -1, 0.5f);
                    LineGUI.INST.add(scrollBarX + 1, scrollerTop + scrollerHeight, 0, -1, 0.5f);
                    LineGUI.INST.drawLines();

                    LineGUI.INST.start(2f);
                    LineGUI.INST.add(scrollBarRight, scrollerTop, 0, 0, 0.5f);
                    LineGUI.INST.add(scrollBarRight, scrollerTop + scrollerHeight, 0, 0, 0.5f);
                    LineGUI.INST.add(scrollBarX, scrollerTop + scrollerHeight, 0, 0, 0.5f);
                    LineGUI.INST.drawLines();
                }
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
        this.height = GameMath.round(this.font.getLineHeight()+4);
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
        }
        if (!isOpen) {
//            System.out.println("popup was closed");
            this.comboBoxList = null;
        }
        Engine.enableDepthMask(false);
        
        AbstractUI popup = gui.getPopup();
        
        this.hovered = this.enabled && ((popup != null && popup == this.comboBoxList) || (popup == null&&  this.mouseOver(i, j))) ;
        
        

//        Shaders.colored.enable();
        renderBox();
        final ITess tessellator = Engine.getTess();
        renderRoundedBoxShadow(this.posX +width - height+1, this.posY+1, 0, this.height-2, this.height-2, 0xeaeaea, hovered ? 1 : 0.7F, false);

        Engine.setPipeStateColored2D();
        int inset = height / 6;
        if (inset < 1)
            inset = 1;
        int inseth = inset;
        tessellator.setColorF(0xeaeaea, 0.7F);
        tessellator.add(this.posX + this.width - height + inset, this.posY + inseth * 2, 0.0f);
        tessellator.add(this.posX + this.width - height + inset + (height - inset * 2) / 2, this.posY + this.height - inseth * 2 + 3, 0.0f);
        tessellator.add(this.posX + this.width - inset+2, this.posY + inseth * 2, 0.0f);
        tessellator.drawTris();
        inset--;
        
        tessellator.setColorF(0, 0.7F);
        tessellator.add(this.posX + this.width - height + inset, this.posY + inseth * 2 + 1, 0.0f);
        tessellator.add(this.posX + this.width - height + inset + (height - inset * 2) / 2, this.posY + this.height - inseth * 2 + 1, 0.0f);
        tessellator.add(this.posX + this.width - inset, this.posY + inseth * 2 + 1, 0.0f);
        tessellator.drawTris();

        int colorDisabled = 0xCCCCCC;
        if (inset < 4) inset = 4;
        this.font.maxWidth = this.width - height - inset;
        String val = String.valueOf(this.value);
        this.font.drawString(val, this.posX + 4, this.posY + this.font.centerY(this.height), this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);

        int w = GameMath.round(this.font.getStringWidth(val));
        if (canexpandHorizontally && !isOpen) {
            this.width = height+inset+w+4;
        }
        this.font.maxWidth = -1;
        if (this.drawTitle) {
            if (titleLeft) {
                this.font.drawString(this.string, this.posX - titleWidth, this.posY  + this.font.centerY(this.height), this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);

            } else {
                this.font.drawString(this.string, this.posX + this.width + 15, this.posY  + this.font.centerY(this.height), this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);
            }

        }
        Engine.enableDepthMask(true);
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
            return false;
        }
        if (!enabled) return false;
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
