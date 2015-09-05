package nidefawl.qubes.font;

import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.TrueTypeFont.GlyphRect;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.ClipboardHelper;

public class TextInput {

    public TrueTypeFont trueType;
    public String       editText;
    public String       searchPattern  = "";
    public boolean      mouseDown      = false;
    public boolean      rightMouseDown = false;
    public boolean      did            = false;
    public int          selStart       = 0;
    public int          selEnd         = 0;
    public int          shiftPX        = 0;
    public int          mpos;
    public int          tick           = 0;
    public String       prevText       = null;
    public List<String> history        = Collections.emptyList();
    public boolean      hasHistory     = false;
    public int          commandScroll  = 0;
    public boolean      focused       = false;
    public FontRenderer font;
    public int          xPos;
    public int          yPos;
    public int          width;
    public int          height;
    private ITextEdit itextedit;

    public TextInput(FontRenderer font, ITextEdit itextedit) {
        this.mpos = 0;
        this.searchPattern = "";
        this.editText = "";
        this.font = font;
        this.trueType = font.trueTypeFont;
        this.itextedit = itextedit;
    }

    public static boolean use17 = false;

    static {
        try {
            use17 = Character.class.getMethod("isAlphabetic", int.class) != null;
        } catch (Exception e) {
            use17 = false;
        }
    }

    public static boolean isAlphabetic(int codePoint) {
        if (use17)
            return Character.isAlphabetic(codePoint) || Character.isWhitespace(codePoint);
        return (((((1 << Character.UPPERCASE_LETTER) | (1 << Character.LOWERCASE_LETTER) | (1 << Character.TITLECASE_LETTER) | (1 << Character.MODIFIER_LETTER)
                | (1 << Character.OTHER_LETTER) | (1 << Character.LETTER_NUMBER)) >> Character.getType(codePoint)) & 1) != 0);
    }

    public void clearPreview() {
        this.prevText = null;
    }

    public void calculatePreview() {
        this.prevText = null;
        if (hasHistory) {
            int s = 0;
            boolean fullmatch = false;
            while (s < history.size()) {
                String sCmd = history.get(history.size() - 1 - s);
                if (sCmd.equals(editText)) {
                    fullmatch = true;
                    break;
                }
                s++;
            }
            if (!fullmatch) {
                s = 0;
                while (s < history.size()) {
                    String sCmd = history.get(history.size() - 1 - s);
                    if (sCmd.startsWith(editText)) {
                        this.prevText = sCmd;
                        return;
                    }
                    s++;
                }
            }
        }
    }


    public void onTextInput(int codepoint) {
        int cursorPos = mpos;
        char c = Character.valueOf((char) (codepoint));
        int w = this.trueType.getCharWidth(c);
        if (w > 0) {
            String s = new String(new char[] {c});
            if (hasSelection()) {
                replaceSelection(s);
            } else {
                insertTextAtCursor(s);
            }
            if (cursorPos != mpos) {
                selEnd = selStart = mpos;
            }
            makeCursorVisible();
            commandScroll = 0;
        }
    }
    
    public boolean onKeyPress(int key, int scanCode, int action, int mods) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return true;
        }
        int cursorPos = mpos;
        boolean shiftDown = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean control = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onEscape();
            //ESCAPE
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER) || (key == GLFW.GLFW_KEY_KP_ENTER)) {// return/enter
            if (hasHistory) {
                int index = history.indexOf(this.editText);
                if (index == -1) {
                    history.add(this.editText);
                } else {
                    history.remove(index);
                    history.add(history.size(), this.editText);
                }
            }
            commandScroll = 0;
            String text = this.editText;
            this.editText = "";
            this.mpos = 0;
            this.shiftPX = 0;
            submit(text);
            clearPreview();
            //        } else if ((Keyboard.isKeyDown(Keyboard.KEY_PAGE_UP)) && (chat.currentTab().getChatScroll() <= chat.currentTab().processed.size() - chat.linesToShow - 1)) {
            //            chat.currentTab().setChatScroll(chat.currentTab().getChatScroll() + (control ? 10 : 1));
            //            shiftDown = false;
            //        } else if ((Keyboard.isKeyDown(Keyboard.KEY_PAGE_DOWN)) && (chat.currentTab().getChatScroll() > 0)) {
            //            chat.currentTab().setChatScroll(chat.currentTab().getChatScroll() - (control ? 10 : 1));
            //            shiftDown = false;
        } else if (hasHistory && key == GLFW.GLFW_KEY_UP && (commandScroll < history.size())) {// chat history up
            commandScroll += 1;
            this.editText = history.get(history.size() - commandScroll);
            this.mpos = this.editText.length();
            this.shiftPX = 0;
            clearPreview();
            shiftDown = false;
        } else if (hasHistory && key == GLFW.GLFW_KEY_DOWN && (commandScroll > 0)) {// chat history down
            commandScroll -= 1;
            if (commandScroll == 0)
                this.editText = "";
            else
                this.editText = history.get(history.size() - commandScroll);
            this.mpos = this.editText.length();
            this.shiftPX = 0;
            clearPreview();
            shiftDown = false;
        } else if (hasHistory && !(control) && key == GLFW.GLFW_KEY_TAB && (history.size() > 0)) {// auto completion
            if (commandScroll == 0) {
                this.searchPattern = this.editText.trim();
            }
            if (shiftDown) {
                while (--commandScroll > 0) {
                    String a = history.get(history.size() - commandScroll);
                    if (!a.isEmpty() && a.startsWith(this.searchPattern)) {
                        this.editText = a;
                        this.mpos = this.selEnd = this.selStart = this.editText.length();
                        clearPreview();
                        return true;
                    }
                }
                if (commandScroll < 0)
                    commandScroll = 0;
            } else
                while (++commandScroll < history.size()) {
                    String a = history.get(history.size() - commandScroll);
                    if (!a.isEmpty() && a.startsWith(this.searchPattern)) {
                        this.editText = a;
                        this.mpos = this.selEnd = this.selStart = this.editText.length();
                        clearPreview();
                        return true;
                    }
                }
            //nothing found / end of results...
            commandScroll = 0;
            this.editText = this.searchPattern;
            this.mpos = this.selEnd = this.selStart = this.editText.length();
            this.searchPattern = "";
            clearPreview();
            return true;
            //        } else if (control && (Keyboard.isKeyDown(Keyboard.KEY_TAB))) {// switch chat tab/channel
            //            if (shiftDown)
            //                chat.previousTab();
            //            else
            //                chat.nextTab();
            //            return;
        } else if (control && key == GLFW.GLFW_KEY_X) {// ctrl +x
            if (hasSelection()) {
                ClipboardHelper.setClipboardString(replaceSelection(""));
            }
        } else if (control && key == GLFW.GLFW_KEY_C) {// ctrl +c
            if (hasSelection()) {
                ClipboardHelper.setClipboardString(getSelection());
            }
        } else if (control && key == GLFW.GLFW_KEY_V) {// ctrl + v
            String var3 = ClipboardHelper.getClipboardString();
            if (var3 == null) {
                var3 = "";
            }
            if (hasSelection()) {
                replaceSelection(var3);
            } else {
                insertTextAtCursor(var3);
            }
        } else if (control && key == GLFW.GLFW_KEY_A) {// ctrl + a
            selStart = 0;
            selEnd = editText.length();
            //            mpos = 0;
            //            cursorPos = 0;
        } else if (key == GLFW.GLFW_KEY_RIGHT) {
            if (!control) {
                if (mpos < editText.length())
                    mpos++;
            } else if (mpos + 1 < editText.length()) {// do fancy space searching
                int i = mpos + 1;
                while (i < editText.length() && editText.charAt(i - 1) != ' ' && editText.charAt(i - 1) != ',')
                    i++;
                mpos = i;
            }
        } else if (key == GLFW.GLFW_KEY_LEFT) {
            if (!control) {
                if (mpos >= 1)
                    mpos--;
            } else if (mpos - 1 > 0) {// do fancy space searching
                int i = mpos - 1;
                while (i > 0 && editText.charAt(i - 1) != ' ' && editText.charAt(i - 1) != ',')
                    i--;
                mpos = i;
            }
        } else if (key == GLFW.GLFW_KEY_HOME) {
            mpos = 0;
            cursorPos--;
        } else if (key == GLFW.GLFW_KEY_END) {
            mpos = this.editText.length();
            cursorPos++;
        } else if (key == GLFW.GLFW_KEY_DELETE && editText.length() > 0) {
            if (hasSelection()) {
                replaceSelection("");
            } else if (this.editText.length() > 0 && mpos < editText.length()) {
                this.editText = this.editText.substring(0, mpos) + editText.substring(mpos + 1);
                calculatePreview();
            }
            commandScroll = 0;
        } else if (key == GLFW.GLFW_KEY_BACKSPACE && editText.length() > 0) {
            if (hasSelection()) {
                replaceSelection("");
            } else if (this.editText.length() > 0 && mpos >= 1) {
                this.editText = this.editText.substring(0, mpos - 1) + editText.substring(mpos);
                mpos--;
                calculatePreview();
            }
            commandScroll = 0;
        } else if (((int) key) >= 32 && scanCode >= 0 && scanCode <= 256) {
//            if (hasSelection()) {
//                replaceSelection(String.valueOf(key));
//            } else {
//                insertTextAtCursor(String.valueOf(key));
//            }
//            commandScroll = 0;
        }
        if (cursorPos != mpos) {
            if (shiftDown) {
                if (cursorPos > mpos) {
                    if (selEnd - mpos > 0)
                        selEnd = mpos;
                    else if (selEnd - mpos < 0)
                        selStart = mpos;
                    else
                        selEnd = selStart = mpos;
                } else {
                    if (selStart - mpos > 0)
                        selEnd = mpos;
                    else if (selStart - mpos < 0)
                        selStart = mpos;
                    else
                        selEnd = selStart = mpos;
                }

            } else {
                selEnd = selStart = mpos;
            }
        }
        makeCursorVisible();
        return true;
    }

    private void submit(String text) {
        itextedit.submit(this, text);
    }

    public void makeCursorVisible() {
        int cursorPosPX = editText.isEmpty() ? 0 : trueType.getWidth(editText.substring(0, mpos));
        int boxWidthPX = getWidth()-2;
        if (shiftPX == 0 || (cursorPosPX > boxWidthPX)) {
            shiftPX = cursorPosPX - boxWidthPX;
        } else if (cursorPosPX < boxWidthPX) {
            shiftPX = 0;
        } else if (cursorPosPX - shiftPX < 0)
            shiftPX = cursorPosPX;
        if (shiftPX < 0)
            shiftPX = 0;
    }

    public boolean hasSelection() {
        return this.editText.length() > 0 && selEnd != selStart && selEnd <= editText.length() && selEnd >= 0 && selStart <= editText.length() && selStart >= 0;
    }

    public String getSelection() {
        String selectedText;
        if (selEnd > selStart) {
            selectedText = editText.substring(selStart, selEnd);// -1 ??
        } else {
            selectedText = editText.substring(selEnd, selStart);// -1 ??
        }
        return selectedText;
    }

    public String replaceSelection(String newText) {
        String selectedText;
        if (selEnd > selStart) {
            selectedText = editText.substring(selStart, selEnd);// -1 ??
            this.editText = editText.substring(0, selStart) + newText + editText.substring(selEnd);
            mpos = selStart + newText.length();
        } else {
            selectedText = editText.substring(selEnd, selStart);// -1 ??
            this.editText = editText.substring(0, selEnd) + newText + editText.substring(selStart);
            mpos = selEnd + newText.length();
        }
        if (mpos > editText.length())
            mpos = editText.length();
        selStart = selEnd = mpos;
        commandScroll = 0;
        clearPreview();
        return selectedText;
    }

    public void insertTextAtCursor(String newText) {
        if (newText == null)
            newText = "";
        for (int i = 0; i < newText.length(); i++) {
            if (!font.isValid(newText.charAt(i))) {
                newText = (i > 0 ? newText.substring(0, i - 1) : "") + newText.substring(i + 1);
            }
        }
        this.editText = this.editText.substring(0, mpos) + newText + editText.substring(mpos);
        mpos += newText.length();
        if (mpos > editText.length())
            mpos = editText.length();
        selStart = selEnd = mpos;
        commandScroll = 0;
        calculatePreview();
    }

    public int getCharPositionFromXCoord(double mouseX) {
        int totalwidth = -shiftPX;
        GlyphRect rect;
        int mX = (int) Math.round(mouseX);
        for (int i = 0; i < editText.length(); i++) {
            int charCurrent = editText.charAt(i);
            if (charCurrent == ' ') {
                int spaceWidth = Math.min(trueType.fontMetrics.charWidth(charCurrent) + trueType.correctL, (int) (trueType.fontMetrics.getHeight() * 0.2D));
                totalwidth += spaceWidth;
            }
            if ((rect = trueType.getRect(charCurrent)) == null) {
                continue;
            }

            if (charCurrent == '\247' && i + 1 < editText.length()) {
                i += 2;
                continue;
            }
            totalwidth += (rect.width - trueType.correctL);

            if (getLeft() + totalwidth >= mX) {
                return i;
            }
        }
        return editText.length();
    }

    public void drawStringWithCursor(double mouseX, double mouseY, boolean mouseDownLeft) {
        this.tick = Game.ticksran;
        int boxWidth = getWidth();
//        boolean overTextBox = boxHeight < mouseY && mouseY < ;
        boolean overTextBoxY = mouseY >= getTop() && mouseY <= getBottom();
        if (mouseDownLeft) {
            if (!did) {
                if (mouseDown) {
                    if (mouseX <= getLeft()) {
                        if (selEnd > 0) {
                            selEnd--;
                            mpos = selEnd;
                            makeCursorVisible();
                        }
                    } else if (mouseX <= getRight()+11) {
                        selEnd = getCharPositionFromXCoord(mouseX);
                        mpos = selEnd;
                    } else if (selEnd < editText.length()) {
                        selEnd++;
                        mpos = selEnd;
                        makeCursorVisible();
                    }
                } else if (getRight()+11 > mouseX&&getLeft() < mouseX && overTextBoxY) {
                    selStart = getCharPositionFromXCoord(mouseX);
                    mpos = selStart;
                    mouseDown = true;
                } else {
                    did = true;
                }
                checkCursor();
            }
        } else {
            mouseDown = false;
            did = false;
        }
//        shiftPX = 2;
//        this.xPos = 30;
//        this.yPos = 30;
//        this.width = 200;
//        this.height = 40;
        if (Mouse.isButtonDown(1)) {
            if (!rightMouseDown) {
                if (overTextBoxY) {
                    if (mouseX <= getRight()) {
                        mpos = getCharPositionFromXCoord(mouseX);
                        insertTextAtCursor(ClipboardHelper.getClipboardString());
                    }
                }
            }
            rightMouseDown = true;
        } else {
            rightMouseDown = false;
        }
        GlyphRect rect;
        int charCurrent;
        boolean showCursor = this.focused && (this.tick / 6 % 2 == 0);
        int totalwidth = 0;
        float startY = -2;
//        GL11.glPushMatrix();
        Shaders.textured.enable();
        Tess tessellator = Tess.instance;
        tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
        tessellator.setColor(-1, 255);
        if (hasSelection() && this.focused) {
            int width = trueType.getWidth(editText.substring(selEnd > selStart ? selStart : selEnd, selEnd > selStart ? selEnd : selStart));
            int widthPre = trueType.getWidth(editText.substring(0, selEnd > selStart ? selStart : selEnd));
            float selRight = getLeft() + widthPre + width;
            float selLeft = getLeft() + widthPre;
            if (selLeft < getLeft()+shiftPX-30) {
                selLeft = getLeft()+shiftPX-30;
            }
            if (selRight > getRight()+shiftPX+30) {
                selRight = getRight()+shiftPX+30;
            }
            GL11.glEnable(3042 /*GL_BLEND*/);
            GL11.glBlendFunc(770, 771);
            Shaders.colored.enable();
            tessellator.setColorRGBAF(1.2F, 0.2F, 1.0F, 0.4F);
            tessellator.add(selLeft, getBottom() + startY + 3.0F, 0.0f);
            tessellator.add(selRight, getBottom() + startY + 3.0F, 0.0f);
            tessellator.add(selRight, getBottom() + startY - height + 3.0F, 0.0f);
            tessellator.add(selLeft, getBottom() + startY - height + 3.0F, 0.0f);
            tessellator.draw(GL11.GL_QUADS);
            tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
            tessellator.setColor(-1, 255);
            Shaders.textured.enable();
            GL11.glDisable(3042 /*GL_BLEND*/);
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, trueType.fontTextureID);
//        tessellator.startDrawingQuads();
//        tessellator.setColorRGBA_F(1, 1, 1, 1);
        int prevColor = -1;
        int i = 0;
        boolean isURL = editText.startsWith("http");
        while (true) {
            if (i >= editText.length()) {
                break;
            }
            if (showCursor && i == mpos) {
                tessellator.draw(GL11.GL_QUADS);
                tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
                tessellator.setColor(-1, 255);
                Shaders.colored.enable();
                GL11.glLineWidth(2.0F);
                tessellator.add(totalwidth + getLeft() + 1F, getBottom() + startY, 0);
                tessellator.add(totalwidth + getLeft() + 1F, getBottom() + startY - height, 0);
                tessellator.draw(GL11.GL_LINES);
                tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
                tessellator.setColor(-1, 255);
                Shaders.textured.enable();
//                if (prevColor >= 0) {
//                    int l = GuiIngameCland.colorMap[prevColor];
//                    tessellator.setColorRGBA_F((float) (l >> 16) / 255F, (float) (l >> 8 & 0xff) / 255F, (float) (l & 0xff) / 255F, 1.0F);
//                } else {
//
//                    tessellator.setColorRGBA_F(1, 1, 1, 1);
//                }
            }
            charCurrent = editText.charAt(i);
            int thisCharWidth;
            if (charCurrent == ' ') {
                thisCharWidth = Math.min(trueType.fontMetrics.charWidth(charCurrent) + trueType.correctL, (int) (trueType.fontMetrics.getHeight() * 0.2D));
                if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                    break;
                }
                totalwidth += thisCharWidth;
            }
            if ((rect = trueType.getRect(charCurrent)) == null) {
                i++;
                continue;
            }

            thisCharWidth = (rect.width - trueType.correctL);
            if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                break;
            }
//            if (!isURL)
//                if (charCurrent == '&' && i + 1 < editText.length()) {
//                    int colorIndex = "0123456789abcdef".indexOf(Character.toLowerCase(editText.charAt(i + 1)));
//                    if (colorIndex >= 0) {
//                        int l = GuiIngameCland.colorMap[colorIndex];
//                        tessellator.setColorRGBA_F((float) (l >> 16) / 255F, (float) (l >> 8 & 0xff) / 255F, (float) (l & 0xff) / 255F, 1.0F);
//                        //                    i += 2;
//                        prevColor = colorIndex;
//                        //                    continue;
//                    }
//                }
            if (totalwidth-shiftPX > -30) {
                rect.drawQuadTess(tessellator, totalwidth + getLeft(), startY + getBottom());
            }
            totalwidth += thisCharWidth;
            i++;
        }
        tessellator.draw(GL11.GL_QUADS);
        tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
        tessellator.setColor(-1, 255);
        if (showCursor && mpos == editText.length()) {
            GL11.glLineWidth(2.0F);
            Shaders.colored.enable();
            tessellator.add(totalwidth + getLeft() + 2F, getBottom() - 1, 0);
            tessellator.add(totalwidth + getLeft() + 2F, getBottom() - height + 1, 0);
            tessellator.draw(GL11.GL_LINES);
            tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
            tessellator.setColor(-1, 255);
            Shaders.textured.enable();
        }
        if (editText.startsWith("/") && shiftPX == 0) {
            if (prevText != null) {
                i = editText.length();
                if (i < prevText.length()) {
                    tessellator.setColorRGBAF(0.7F, 0.7F, 0.7F, 0.8F);
                    while (true) {
                        if (i >= prevText.length()) {
                            break;
                        }
                        charCurrent = prevText.charAt(i);
                        int thisCharWidth;
                        if (charCurrent == ' ') {
                            thisCharWidth = Math.min(trueType.fontMetrics.charWidth(charCurrent) + trueType.correctL, (int) (trueType.fontMetrics.getHeight() * 0.2D));
                            if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                                break;
                            }
                            totalwidth += thisCharWidth;
                        }
                        if ((rect = trueType.getRect(charCurrent)) == null) {
                            i++;
                            continue;
                        }

                        thisCharWidth = (rect.width - trueType.correctL);
                        if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                            break;
                        }
                        rect.drawQuadTess(tessellator, totalwidth + getLeft(), startY + getBottom());
                        totalwidth += thisCharWidth;
                        i++;
                    }
                    tessellator.draw(GL11.GL_QUADS);
                    tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
                    tessellator.setColor(-1, 255);
                    String hint = "Press <TAB> to complete";
                    int w = trueType.getWidth(hint);
                    if (getWidth() - totalwidth > w + 40) {
                        i = 0;
                        totalwidth = getWidth() - w;
                        tessellator.setColorRGBAF(0.7F, 0.7F, 0.7F, 0.8F);
                        while (true) {
                            if (i >= hint.length()) {
                                break;
                            }
                            charCurrent = hint.charAt(i);
                            int thisCharWidth;
                            if (charCurrent == ' ') {
                                thisCharWidth = Math.min(trueType.fontMetrics.charWidth(charCurrent) + trueType.correctL,
                                        (int) (trueType.fontMetrics.getHeight() * 0.2D));
                                if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                                    break;
                                }
                                totalwidth += thisCharWidth;
                            }
                            if ((rect = trueType.getRect(charCurrent)) == null) {
                                i++;
                                continue;
                            }

                            thisCharWidth = (rect.width - trueType.correctL);
                            if (totalwidth - shiftPX + thisCharWidth > getWidth()) {
                                break;
                            }
                            rect.drawQuadTess(tessellator, totalwidth + getLeft(), startY + getBottom());
                            totalwidth += thisCharWidth;
                            i++;
                        }
                        tessellator.draw(GL11.GL_QUADS);
                        tessellator.setOffset(-shiftPX, 0.0F, 0.0F);
                        tessellator.setColor(-1, 255);
                    }
                }
            }
        }
        tessellator.setOffset(0, 0, 0);
        tessellator.setColor(-1, 255);
    }
    
    private void checkCursor() {
        if (this.mpos > this.editText.length())
            this.mpos = this.editText.length();
        if (this.mpos < 0)
            this.mpos = 0;
    }

    private void onEscape() {
        itextedit.onEscape(this);
    }

    public int getLeft() {
        return this.xPos;
    }
    public int getTop() {
        return this.yPos;
    }
    public int getWidth() {
        return this.width;
    }
    public int getHeight() {
        return this.height;
    }
    public int getRight() {
        return getLeft()+getWidth();
    }
    public int getBottom() {
        return getTop()+getHeight();
    }
}
