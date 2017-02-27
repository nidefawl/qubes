package nidefawl.qubes.font;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;

public class FontRenderer {
    public static HashMap<String, FontRenderer> fonts = new HashMap<String, FontRenderer>();
    public static void destroy() {
        if (fonts.isEmpty()) {
            return;
        }
        for (FontRenderer f : fonts.values()) {
            f.free();
        }
        fonts.clear();
    }

	private void free() {
	    this.trueTypeFont.release();
	    this.trueTypeFont=null;
    }

    public TrueTypeFont trueTypeFont;
	private float size;
	private int style;
    private int font;
    public int maxWidth = -1;
    public float drawedHeight = 0;
    public float shadowOffset = 0.8F;
	
	public static FontRenderer get(int font, float size, int style) {
        String hashName = font+","+size+","+style;
		FontRenderer r = fonts.get(hashName);
		if (r == null) {
			r = new FontRenderer(font, size, style);
			fonts.put(hashName, r);
		}
		return r;
	}
	public static void init() {
//	    for (String s : ttfFonts) {
//	        AssetBinary bin = AssetManager.getInstance().loadBin("fonts/"+s+".ttf");
//	        ByteArrayInputStream bis = new ByteArrayInputStream(bin.getData());
//	        try {
//                Font f = Font.createFont(Font.TRUETYPE_FONT, bis);
//                ttfMap.put(s.toLowerCase(), f);
//            } catch (FontFormatException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//	    }
	}


    public FontRenderer(int font, float size, int style) {
        this.font = font;
        this.size = size;
        this.style = style;
        this.setupFont();
        while (!trueTypeFont.isValid()&&this.size>0) {
            this.size--;
            setupFont();
        }
    }

    final static String[] fontNames = {
        "OpenSans-Regular.ttf",
        "OpenSans-Bold.ttf",
        "OpenSans-Italic.ttf",
        "OpenSans-BoldItalic.ttf",
    };
    private void setupFont() {
        if (this.trueTypeFont != null) {
            this.trueTypeFont.release();
            this.trueTypeFont = null;
        }
//        this.trueTypeFont = new TrueTypeFontAWT(this.fontName, this.size, this.style, true);
//        if (this.lineHeight == -1)
//            this.lineHeight = (int) (this.trueTypeFont.getLineHeight() * 0.8);
        String s = "fonts/"+fontNames[this.style&3];
        this.trueTypeFont = new TrueTypeFont(s, this.size*1.5f, this.style, true);
    }
    

    public float drawString(final String chatline, final float x, final float y, final int color, final boolean shadow, final float alpha) {
        return this.drawString(chatline, x, y, color, shadow, alpha, TrueTypeFont.ALIGN_LEFT);
    }

    public float drawString(final String chatline, final float x, final float y, int color, final boolean shadow, final float alpha, final int alignment) {
        this.drawedHeight = 0;
        if (chatline == null || chatline.length() == 0)
            return 0;
        if ((color & 0xff000000) == 0) {
            color |= 0xff000000;
        }
        ITess tess = Engine.getFontTess();
        if (shadow) {
            final int k2 = (color & 0xf0f0f0) >> 2 | color & 0xff000000;
            tess.setColorF(k2, alpha*0.8f);
            this.trueTypeFont.drawString(tess, x + this.shadowOffset, y + this.shadowOffset, chatline, alignment, false, alpha, maxWidth, k2, alpha*0.8f);
        }
        tess.setColorF(color, alpha);
        float w = this.trueTypeFont.drawString(tess, x, y, chatline, alignment, true, alpha, maxWidth, color, alpha);
        this.trueTypeFont.drawTextBuffer(tess);
        this.drawedHeight = this.trueTypeFont.getLastDrawHeight();
        return w;
    }
    
    public int getTexture() {
        return this.trueTypeFont.getTexture();
    }


    public float getStringWidth(final String s) {
        return this.trueTypeFont.getWidth(s);
    }


    /**
     * @return the lineHeight
     */
    public float getCharHeight() {
        return this.trueTypeFont.getCharHeight();
    }


    /**
     * @return the lineHeight
     */
    
    public float getLineHeight() {
        return this.trueTypeFont.getLineHeight();
    }

    public boolean isValid(char charAt) {
        return trueTypeFont.hasCharacter(charAt);
    }
    public int centerY(int height) {
        return GameMath.round(height-(height-this.trueTypeFont.getCharHeight())/2-0.2f);
    }
    public TrueTypeFont getTTF() {
        return this.trueTypeFont;
    }
}
