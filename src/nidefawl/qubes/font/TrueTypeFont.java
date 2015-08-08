package nidefawl.qubes.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.texture.TextureManager;

import org.lwjgl.opengl.GL11;

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's
 * Engine
 *
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 * @new version edited by David Aaron Muhar (bobjob)
 */
public class TrueTypeFont {
    public final static int[] colorMap = new int[32];
    static {
        for (int l = 0; l < 32; l++) {
            int j1 = (l >> 3 & 1) * 85;
            int l1 = (l >> 2 & 1) * 170 + j1;
            int j2 = (l >> 1 & 1) * 170 + j1;
            int l2 = (l >> 0 & 1) * 170 + j1;
            if (l == 6) {
                l1 += 85;
            }
            if (l >= 16) {
                l1 /= 2;
                j2 /= 2;
                l2 /= 2;
            }
            colorMap[l] = (l1 & 0xff) << 16 | (j2 & 0xff) << 8 | l2 & 0xff;
        }
    }


    public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
    /**
     * Array that holds necessary information about the font characters
     */
    // private CharPosition[] charArray = new CharPosition[256];
    public final static String ctrls = "0123456789abcdefs";
    /**
     * Default font texture width
     */
    private final int texture;
    /**
     * Font's height
     */
    public int fontHeight = 0;
    /** A reference to Java's AWT Font that we create our font texture from */
    /**
     * Texture used to cache the font 0-255 characters
     */
    public int fontTextureID;
    /**
     * The font metrics for our Java AWT font
     */
    public FontMetrics fontMetrics;
    public int correctL = 1;
    public int descent;
    public int ascent;
    public int usedTextureHeight;
    public int maxAscent;
    public int spaceWidth;
    boolean useAA = false;
    BufferedImage image = null;
    int drawedHeight;
    private boolean valid;
    private GlyphRect[] rects;
    private HashMap<Integer, GlyphRect> charMap = new HashMap<Integer, GlyphRect>();
    private Font font = null;
    private int[] usedY;
    private int offset;

    public TrueTypeFont(final Font font, int from, int to) {
        this.texture = 512;
        this.valid = this.createSet(this.texture, this.texture, font, from, to, 0);
    }

    public TrueTypeFont(final Font font, final boolean useAA) {
        this.texture = 512;
        this.useAA = useAA;
        this.valid = this.createSet(this.texture, this.texture, font, 0 + 32, 256 + 32, 32);
    }

    public TrueTypeFont(final Font font, int texSize, boolean antialiasing) {
        this.texture = texSize;
        this.useAA = antialiasing;
        this.valid = this.createSet(this.texture, this.texture, font, 0 + 32, 256 + 32, 32);
    }

    public static int getControlChar(char chr) {
        return ctrls.indexOf(chr);
    }

    public static boolean isSupported(final String fontname) {
        final Font font[] = getFonts();
        for (int i = font.length - 1; i >= 0; i--) {
            if (font[i].getName().equalsIgnoreCase(fontname))
                return true;
        }
        return false;
    }

    public static Font[] getFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    }

    public static byte[] intToByteArray(final int value) {
        return new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public void setCorrection(final boolean on) {
        if (on) {
            this.correctL = 2;
//            this.correctR = 1;
        } else {
            this.correctL = 0;
//            this.correctR = 0;
        }
    }

    public void drawGlyph(final GlyphRect rect, float xDrawOffset, float yDrawOffset, Shape glyphShape, final Graphics2D g, final boolean useDrawString) {
        final float offY = yDrawOffset - rect.yoffset;
        if (useDrawString) {
            g.drawString(Character.toString(rect.ch), xDrawOffset, offY);
        } else {
            g.translate(xDrawOffset, offY);
            g.fill(glyphShape);
            g.translate(-xDrawOffset, -offY);
        }
    }

    private boolean createSet(final int width, final int height, final Font font, int startChar, int endChar, int offset) {
        this.font = font;
        Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
        map.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        this.font = font.deriveFont(map);
        final boolean useDrawString = true;
        final Padding padding = new Padding(1, 1, 1, 1, 0);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, useAA ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, useAA ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        g.setFont(this.font);
//        Thread.dumpStack();

        final FontRenderContext fontRenderContext = g.getFontRenderContext();
        this.fontMetrics = g.getFontMetrics();
        this.ascent = this.fontMetrics.getMaxAscent();
        this.descent = this.fontMetrics.getMaxDescent();
        this.fontHeight = this.fontMetrics.getLeading() + this.ascent + this.descent;
        this.maxAscent = this.ascent;


        final ArrayList<GlyphRect> rectList = new ArrayList<GlyphRect>();
        final char[] chBuffer = new char[1];
        HashMap<GlyphRect, Float> xDrawOffsets = new HashMap<TrueTypeFont.GlyphRect, Float>();
        HashMap<GlyphRect, Float> yDrawOffsets = new HashMap<TrueTypeFont.GlyphRect, Float>();
        HashMap<GlyphRect, Shape> outlines = new HashMap<TrueTypeFont.GlyphRect, Shape>();
        int codepoint = startChar - 1;
        while (codepoint++ < endChar) {

            chBuffer[0] = (char) (codepoint);
            if (!font.canDisplay(chBuffer[0])) {
                continue;
            }
            
            boolean newMode = true;
            final GlyphVector vector = font.layoutGlyphVector(fontRenderContext, chBuffer, 0, 1, Font.LAYOUT_LEFT_TO_RIGHT);
            final GlyphMetrics metrics = vector.getGlyphMetrics(0);
            final Rectangle bounds = metrics.getBounds2D().getBounds();

            final int advance = (int) -Math.floor(bounds.getMaxY());
            int glyphWidth = bounds.width + (newMode?bounds.x:1) + padding.left + padding.right;
            final int glyphHeight = bounds.height + 1 + padding.top + padding.bottom;

            final float xoffset = newMode ? -bounds.x/2F : (1-bounds.x);// - bounds.x;
            final int yoffset = bounds.y - 1;

            if (chBuffer[0] == ' ' && glyphWidth == 0)
                glyphWidth = this.fontMetrics.charWidth(' ');
            final GlyphRect rect = new GlyphRect(chBuffer[0], Math.min(glyphWidth, width), glyphHeight, advance + padding.advance, yoffset);
            if (advance < 0)
                this.fontHeight = Math.max(glyphHeight - advance, this.fontHeight);
            else
                this.fontHeight = Math.max(glyphHeight, this.fontHeight);

            xDrawOffsets.put(rect, xoffset + padding.left);
            yDrawOffsets.put(rect, (float) padding.top);
            outlines.put(rect, vector.getGlyphOutline(0));
            rectList.add(rect);
            charMap.put((int) chBuffer[0], rect);
        }

        // sorting of arrays is more efficient then sorting of collections
        final int numGlyphs = rectList.size();
        this.rects = rectList.toArray(new GlyphRect[numGlyphs]);
        Arrays.sort(this.rects, new Comparator<GlyphRect>() {
            @Override
            public int compare(final GlyphRect a, final GlyphRect b) {
                int diff = b.height - a.height;
                if (diff == 0) {
                    diff = b.width - a.width;
                }
                return diff;
            }
        });
        g.setColor(Color.white);

        int xp = 0;
        int dir = 1;
        usedY = new int[width];
        this.usedTextureHeight = 0;

        for (int i = 0; i < numGlyphs; i++) {
            final GlyphRect rect = this.rects[i];

            if (dir > 0) {
                if (xp + rect.width > width) {
                    xp = width - rect.width;
                    dir = -1;
                }
            } else {
                xp -= rect.width;
                if (xp < 0) {
                    xp = 0;
                    dir = 1;
                }
            }

            int yp = 0;
            for (int x = 0; x < rect.width; x++) {
                yp = Math.max(yp, usedY[xp + x]);
            }
            rect.setXY(xp, yp, texture);

            final Graphics2D gGlyph = (Graphics2D) g.create(xp, yp, rect.width, rect.height);
            try {
                float xDrawOffset = xDrawOffsets.get(rect);
                float yDrawOffset = yDrawOffsets.get(rect);
                Shape outline = outlines.get(rect);
                drawGlyph(rect, xDrawOffset, yDrawOffset, outline, gGlyph, useDrawString);
            } finally {
                gGlyph.dispose();
            }

            yp += rect.height + 1;
            for (int x = 0; x < rect.width; x++) {
                usedY[xp + x] = yp;
            }

            if (yp > this.usedTextureHeight) {
                this.usedTextureHeight = yp;
            }
            if (this.usedTextureHeight > height) {
                break;
            }

            if (dir > 0) {
                xp += rect.width + 1;
            } else {
                xp -= 1;
            }
        }
        if (image != null) {
            this.fontTextureID = TextureManager.getInstance().makeNewTexture(image, false, true);
        }
        this.spaceWidth = Math.min(this.fontMetrics.charWidth(' '), (int) (this.fontMetrics.getHeight() * 0.2D));
        int max = 0;
        for (Integer key : this.charMap.keySet()) {
            if (key > max)
                max = key;
        }
        this.offset = offset;
        this.rects = max +1 >= startChar ? new GlyphRect[(max + 1) - startChar] : new GlyphRect[0];
        for (int a = 0; a < rects.length && a < max - startChar; a++) {
            this.rects[a] = this.charMap.get(a + startChar);
        }
        return this.rects.length > 0;
    }

    @SuppressWarnings("unused")
    private int createChar(final int character) {
        if (!font.canDisplay((char) character))
            return -1;
        final char[] chBuffer = new char[] {(char) character};

        final Padding padding = new Padding(1, 1, 1, 1, 0);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, useAA ?
        RenderingHints.VALUE_ANTIALIAS_ON :
        RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, useAA ?
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        g.setFont(font);

        final FontRenderContext fontRenderContext = g.getFontRenderContext();

        final GlyphVector vector = font.layoutGlyphVector(fontRenderContext, chBuffer, 0, 1, Font.LAYOUT_LEFT_TO_RIGHT);
        final GlyphMetrics metrics = vector.getGlyphMetrics(0);
        final Rectangle bounds = metrics.getBounds2D().getBounds();

        final int advance = (int) -Math.floor(bounds.getMaxY());
        int glyphWidth = bounds.width + 1 + padding.left + padding.right;
        final int glyphHeight = bounds.height + 1 + padding.top + padding.bottom;

        final int xoffset = 1 - bounds.x;
        final int yoffset = bounds.y - 1;

        if (chBuffer[0] == ' ' && glyphWidth == 0)
            glyphWidth = this.fontMetrics.charWidth(' ');
        final GlyphRect newRect = new GlyphRect(chBuffer[0], Math.min(glyphWidth, texture), glyphHeight, advance + padding.advance, yoffset);

        if (findSpot(newRect)) {
            // found enough room...
            g.setColor(Color.white);
            final Graphics2D gGlyph = (Graphics2D) g.create(newRect.x, newRect.y, newRect.width, newRect.height);
            try {
                drawGlyph(newRect, xoffset + padding.left, padding.top, vector.getGlyphOutline(0), gGlyph, true);
            } finally {
                gGlyph.dispose();
            }
            charMap.put((int) chBuffer[0], newRect);
            this.unallocate();
            this.fontTextureID = TextureManager.getInstance().makeNewTexture(image, false, true);
            return chBuffer[0];
        }
        return -4;
    }

    private boolean findSpot(GlyphRect newRect) {

        int y = 0;
        int x = 0;
        while (x < this.texture) {
            y = usedY[x] + 1;
            if (texture - y > newRect.height + 1) {
                if (isRoom(newRect, x, y)) {
                    newRect.setXY(x, y, texture);
                    for (int aX = x; aX < x + newRect.width; aX++) {
                        usedY[aX] = newRect.y + newRect.height;
                    }
                    return true;
                }
            }
            x++;
        }
        return false;
    }

    private boolean isRoom(GlyphRect newRect, int x, int y) {
        for (int aX = x; aX < x + newRect.width; aX++) {
            if (y <= usedY[aX]) return false;
        }
        return true;
    }

    public int getWidth(final String whatchars) {
        int totalwidth = 0;
        GlyphRect rect = null;
        String toLower = null;
        for (int i = 0; i < whatchars.length(); i++) {
            final int charCurrent = whatchars.charAt(i);

            if (charCurrent == ' ') {
                final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                totalwidth += spaceWidth;
            }

            if (charCurrent == '\u00A7' && i + 1 < whatchars.length()) {
                char c2 = whatchars.charAt(i + 1);
                if (c2 == 's') {
                    final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                    totalwidth += spaceWidth;
                }
                if (c2 == 'u') {
                    i+=6;
                }
                i++;
                continue;
            }
            if (charCurrent == 0 && i + 1 < whatchars.length()) {
                if (toLower == null) {
                    toLower = whatchars.toLowerCase();
                }
                char chr = toLower.charAt(i + 1);
                if (chr == 'u' && i + 2 + 6 < whatchars.length()) {
                    String hex = whatchars.substring(i + 2, i + 2 + 6);
                    int rgb = 0xFFFFFFFF;
                    try {
                        rgb = Integer.parseInt(hex, 16);//SLOW :(
                    } catch (NumberFormatException ignored) {
                    }
                    if (rgb >= 0 && rgb <= 0xFFFFFF) {
                    }
                    i += 7;
                    continue;
                } else if (chr == 'z') {
//                    ctrl = 1;
                    i += 1;
                } else if (chr == 's') {
                    final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                    totalwidth += spaceWidth;
                    i += 1;
                } else {
                    int j = getControlChar(chr);
                    if (j == 16) {
                        totalwidth += this.spaceWidth;
                    } else if (j >= 0 && j <= 15) {
                    }
                    i += 2;
                    continue;
                }
            }
            if ((rect = this.getRect(charCurrent)) == null) {
                continue;
            }

            totalwidth += (rect.width - this.correctL);

        }
        return (totalwidth);
    }

    public int getWidth2(final String whatchars) {
        if (whatchars.contains("\n")) {
            String split[] = whatchars.split("\n");
            if (split.length > 1) {
                int max = getWidth(split[0]);
                for (int i = 1; i < split.length; i++) ;
                {
                    int l2 = getWidth(split[1]);
                    if (l2 > max) max = l2;
                }
                return max;
            }
        }
        return getWidth(whatchars);
    }

    public int getCharWidth(final Character c) {
        if (c == ' ')
            return Math.min(this.fontMetrics.charWidth(c) - this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
        return this.getRectWidth(c);
    }

    public int getGlyphWidth(final int c) {
        return this.getRectWidth(c);
    }
    public int getGlyphHeight(final int c) {
        return this.getRectHeight(c);
    }

    public int getHeight() {
        return this.fontHeight;
    }

    public int getHeight(final String HeightString) {
        return this.fontHeight;
    }

    public int getLineHeight() {
        return this.fontHeight;
    }


    public int drawGlyph(final float x, final float y, final int index, final boolean shadow, final float alpha) {
        GlyphRect rect = this.getRect(index);
        if (rect == null) return 0;
        float startY = -2;
        Tess tess = Tess.instance2;
        rect.drawQuadTess(tess, x, startY + y);
        return (rect.width - this.correctL);
    }

    public String trimColorChars(final String in) {
        return in.replaceAll("(\u00A7([a-f0-9]))", "");
    }

    public int drawString(Tess tess, final float x, final float y, final String whatchars, final int format, final boolean shadow, final float alpha, int maxWidth) {
        final int startIndex = 0;
        final int endIndex = whatchars.length() - 1;
        GlyphRect rect = null;
        int charCurrent;
        int totalwidth = 0;
        int i = startIndex, d, c;
        float startY = -2;

        switch (format) {
            case ALIGN_RIGHT: {
                d = 1;
                c = this.correctL;
                totalwidth -= getWidth(whatchars) + c;
                break;
            }
            case 3: {
                totalwidth = getWidth(whatchars);
                totalwidth /= -2;
            }
            d = 1;
            c = this.correctL;
            break;
            case ALIGN_CENTER: {
                totalwidth = 0;
                final String colorFreeString = this.trimColorChars(whatchars);
                int offset = 0;
                for (int l = i + offset; l <= Math.min(endIndex, colorFreeString.length() - offset - 1); l++) {
                    charCurrent = colorFreeString.charAt(l);
                    if (charCurrent == '\n')
                        break;
                    if (charCurrent == ' ') {
                        final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                        totalwidth += spaceWidth;
                    }

                    if (charCurrent == '\u00A7' && l + 1 < whatchars.length()) {
                        char c2 = whatchars.charAt(l + 1);
                        if (c2 == 's') {
                            final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                            totalwidth += spaceWidth;
                        }
                        if (c2 == 'u') {
                            l+=6;
                        }
                        l++;
                        continue;
                    }
                    rect = this.getRect(charCurrent);
                    if (rect != null)
                        totalwidth += rect.width - this.correctL;
                }
                totalwidth /= -2;
            }
            case ALIGN_LEFT:
            default: {
                d = 1;
                c = this.correctL;
                break;
            }
        }
        String toLower = null;
        char ctrl = 0;
        this.drawedHeight = this.fontHeight * d;
        while (i >= startIndex && i <= endIndex) {
            charCurrent = whatchars.charAt(i);
            if (charCurrent == ' ') {
                totalwidth += this.spaceWidth;
            }

            if (charCurrent == '\n') {
                startY += this.fontHeight * d;
                this.drawedHeight += this.fontHeight * d;
                totalwidth = 0;
                if (format == ALIGN_CENTER) {
                    for (int l = i + 1; l <= endIndex; l++) {
                        Character c2 = whatchars.charAt(l);
                        if (c2 == '\n')
                            break;
                        rect = this.getRect(c2);
                        if (rect != null)
                            totalwidth += rect.width - this.correctL;
                    }
                    totalwidth /= -2;
                }
                // if center get next lines total width/2;
            }
            if (charCurrent == 0 && i + 1 < whatchars.length()) {
                if (toLower == null) {
                    toLower = whatchars.toLowerCase();
                }
                char chr = toLower.charAt(i + 1);
                if (chr == 'u' && i + 2 + 6 < whatchars.length()) {
                    String hex = whatchars.substring(i + 2, i + 2 + 6);
                    int rgb = 0xFFFFFFFF;
                    try {
                        rgb = Integer.parseInt(hex, 16);//SLOW :(
                    } catch (NumberFormatException ignored) {
                    }
                    if (rgb >= 0 && rgb <= 0xFFFFFF) {
                        if (shadow) {
//                        OpenGlHelper.glColorIntRGB(0xFF000000|rgb);
                            tess.setColor(rgb, 0xFF);
                        }
                    }
                    i += 8;
                    continue;
                } else if (chr == 'z') {
                    ctrl = 1;
                    i += 1;
                } else if (chr == 's') {
                    final int spaceWidth = Math.min(this.fontMetrics.charWidth(charCurrent) + this.correctL, (int) (this.fontMetrics.getHeight() * 0.2D));
                    totalwidth += spaceWidth;
                    i += 1;
                } else {
                    int j = getControlChar(chr);
                    if (j == 16) {
                        totalwidth += this.spaceWidth;
                    } else if (j >= 0 && j <= 15) {
                        if (!shadow) {
                            j += 16;
                        }
                        final int l = colorMap[j];
                        tess.setColorF(l, alpha);
                    }
                    i += 2;
                    continue;
                }
            }
            if ((rect = this.getRect(charCurrent)) == null) {
                i++;
                continue;
            }
            if (d < 0) {
                totalwidth += (rect.width - c) * d;
            }
            
            rect.drawQuadTess(tess, totalwidth + x, startY + y);
            if (d > 0) {
                if (ctrl != 0) {
                    GlyphRect rect2 = null;
                    if ((rect2 = this.getRect('|')) == null) {
                        i++;
                        continue;
                    }
                    int w = rect2.width / 2 - 2;
                    rect2.drawQuadTess(tess, totalwidth + x + w, startY + y);
                    ctrl = 0;
                }
                totalwidth += (rect.width - c) * d;
            }
            if (maxWidth > 0 && totalwidth >= maxWidth) break;
            i += d;
        }
        return totalwidth;
    }

    public void unallocate() {
        if (this.fontTextureID>0) {
            GL11.glDeleteTextures(this.fontTextureID);
            this.fontTextureID = 0;
        }
    }
    
    
    public void setHeight() {
        this.fontHeight++;
    }

    /**
     * @return the rects
     */
    public GlyphRect getRect(int currentChar) {
        if (currentChar == '\u00A7') return null;
        currentChar -= this.offset;
        GlyphRect rect = currentChar >= 0 && this.rects.length > currentChar ? this.rects[currentChar] : null;
        return rect;
    }

    int getRectWidth(final int i) {
        GlyphRect rect = getRect((char) i);
        return rect == null ? 0 : rect.width;
    }
    int getRectHeight(final int i) {
        GlyphRect rect = getRect((char) i);
        return rect == null ? 0 : rect.height;
    }

    public float getSpaceWidth() {
        return this.spaceWidth;
    }

    public boolean isValid() {
        return valid;
    }

    public class GlyphRect {

        public final char ch;
        public final int width;
        public final int height;
        public final int advance;
        public final int yoffset;
        public int x;
        public int y;
        public float tY2;
        public float tX2;
        public float tY1;
        public float tX1;

        public GlyphRect(final char ch, final int width, final int height, final int advance, final int yoffset) {
            this.ch = ch;
            this.width = width;
            this.height = height;
            this.advance = advance;
            this.yoffset = yoffset;
        }

        public void setXY(int xp, int yp, int textureSize) {
            this.x = xp;
            this.y = yp;
            this.tX1 = this.x / (float) textureSize;
            this.tY1 = this.y / (float) textureSize;
            this.tX2 = this.tX1 + this.width / (float) textureSize;
            this.tY2 = this.tY1 + this.height / (float) textureSize;
        }

        public void drawQuadTess(final Tess tess, final float drawX, float drawY) {
            drawY -= this.advance;
          tess.add(drawX + this.width, drawY, 0F, this.tX2, this.tY2);
          tess.add(drawX + this.width, drawY - this.height, 0F, this.tX2, this.tY1);
          tess.add(drawX, drawY - this.height, 0F, this.tX1, this.tY1);
          tess.add(drawX, drawY, 0F, this.tX1, this.tY2);
        }
    }

    public class Padding {

        public final int top;
        public final int left;
        public final int right;
        public final int bottom;
        public final int advance;

        public Padding(final int top, final int left, final int bottom, final int right, final int advance) {
            this.top = top;
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.advance = advance;
        }

        public Padding max(final Padding other) {
            return new Padding(Math.max(this.top, other.top), Math.max(this.left, other.left), Math.max(this.bottom, other.bottom), Math.max(this.right, other.right), Math.max(this.advance, other.advance));
        }
    }

}