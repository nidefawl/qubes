package nidefawl.qubes.font;

import java.awt.Font;
import java.util.HashMap;

import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.util.GameError;

import org.lwjgl.opengl.GL11;

public class FontRenderer {
	public static HashMap<String, FontRenderer> fonts = new HashMap<String, FontRenderer>();

	public TrueTypeFont trueTypeFont;
	private int lineHeight;
	private float size;
	private int style;
	private String fontName;
	
	public static FontRenderer get(String fontName, float size, int style, int lineHeight) {
		String hashName = fontName.trim().toLowerCase()+","+size+","+style+","+lineHeight;
		FontRenderer r = fonts.get(hashName);
		if (r == null) {
			r = new FontRenderer(fontName, size, style, lineHeight);
			fonts.put(hashName, r);
		}
		return r;
	}


    public FontRenderer(String fontName, float size, int style, int lineHeight) {
        this.fontName = fontName;
        this.size = size;
        this.style = style;
        this.lineHeight = lineHeight;
        this.setupFont();
        while (!trueTypeFont.isValid()) {
            this.size--;
            setupFont();
        }
    }

    public Font getFont() {
        Font font = Font.decode(this.fontName);
        if (font == null) {
        	throw new GameError("Failed creating font "+this.fontName+", "+this.size+", "+this.style);
        }
        return font.deriveFont(this.style, this.size);
    }
    
    private void setupFont() {
        if (this.trueTypeFont != null) {
            this.trueTypeFont.unallocate();
        }
        Font f = getFont();
        this.trueTypeFont = new TrueTypeFont(f, true);
        if (this.lineHeight == -1)
            this.lineHeight = (int) (this.trueTypeFont.getLineHeight() * 0.8);
    }

    public int drawString(final String chatline, final int x, final int y, final int color, final boolean shadow, final float alpha) {
        return this.drawString(chatline, x, y, color, shadow, alpha, TrueTypeFont.ALIGN_LEFT);
    }

    public int maxWidth = -1;
    public int drawedHeight = 0;
    public float shadowOffset = 0.8F;
    public int drawString(final String chatline, final float x, final float y, int color, final boolean shadow, final float alpha, final int alignment) {
        this.drawedHeight = 0;
        if (chatline == null || chatline.length() == 0)
            return 0;
        if ((color & 0xff000000) == 0) {
            color |= 0xff000000;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.trueTypeFont.fontTextureID);
        Tess tess = Tess.instance2;
        if (shadow) {
            final int k2 = (color & 0xf0f0f0) >> 2 | color & 0xff000000;
            tess.setColorF(k2, alpha);
            this.trueTypeFont.drawString(tess, x + this.shadowOffset, y + this.shadowOffset, chatline, alignment, false, alpha, maxWidth);
        }
        tess.setColorF(color, alpha);
        int w = this.trueTypeFont.drawString(tess, x, y, chatline, alignment, true, alpha, maxWidth);
        tess.draw(GL11.GL_QUADS);
        this.drawedHeight = this.trueTypeFont.drawedHeight;
        return w;
    }



    public int drawGlyph(final int index, final int x, final int y, int color, final boolean shadow, final float alpha) {
        if ((color & 0xff000000) == 0) {
            color |= 0xff000000;
        }
        if (shadow) {
            final int k2 = (color & 0xf0f0f0) >> 2 | color & 0xff000000;
            GL11.glColor4f((k2 >> 16 & 0xff) / 255F, (k2 >> 8 & 0xff) / 255F, (k2 & 0xff) / 255F, alpha);
            this.trueTypeFont.drawGlyph(x + 0.8F, y + 0.8F, index, false, alpha);
        }
        GL11.glColor4f((color >> 16 & 0xff) / 255F, (color >> 8 & 0xff) / 255F, (color & 0xff) / 255F, alpha);

        return this.trueTypeFont.drawGlyph(x, y, index, true, alpha);
    }

    public int drawGlyph(final int index, final int x, final int y, final int color, final boolean shadow) {
        return this.drawGlyph(index, x, y, color, shadow, ((color >> 24 & 0xff) == 0 ? 255 : color >> 24 & 0xff) / 255F);
    }

    public int getStringWidth(final String s) {
        return this.trueTypeFont.getWidth(s);
    }

    public int getCharWidth(final Character c) {
        return this.trueTypeFont.getCharWidth(c);
    }

    public int getGlyphWidth(final int c) {
        return this.trueTypeFont.getGlyphWidth(c);
    }

    public int getHeight() {
        return this.trueTypeFont.getHeight();
    }

    public int getAscent() {
        return this.trueTypeFont.ascent;
    }

    public int getDescent() {
        return this.trueTypeFont.descent;
    }

    public void increaseSize() {
        this.lineHeight = -1;
        this.size++;
        setupFont();
        if (!trueTypeFont.isValid()) {
            this.size--;
            setupFont();
        }
    }

    public void decreaseSize() {
        this.lineHeight = -1;
        this.size--;
        setupFont();
        if (!trueTypeFont.isValid()) {
            this.size++;
            setupFont();
        }
    }


    public void setFont(final Font font) {
        this.size = 16F;
        this.style = 0;
        this.lineHeight = -1;
        this.fontName = font.getName();
        setupFont();
    }


    /**
     * @return the lineHeight
     */
    public int getLineHeight() {
        return this.lineHeight;
    }

    /**
     * @return the lineHeight
     */
    public int decLineHeight() {
        return --this.lineHeight;
    }

    /**
     * @return the lineHeight
     */
    public int incLineHeight() {
        return ++this.lineHeight;
    }

    /**
     * @param lineHeight
     *            the lineHeight to set
     */
    public void setLineHeight(final int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public String getFontName() {
        return this.fontName;
    }

    public boolean isValid(char charAt) {
        return trueTypeFont.getRect(charAt) != null;
    }

}