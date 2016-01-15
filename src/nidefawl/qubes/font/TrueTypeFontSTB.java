package nidefawl.qubes.font;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.stb.STBTTPackedchar.Buffer;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class TrueTypeFontSTB extends TrueTypeFont {
    int texW=512;
    public int correctL = 1;
    private Buffer chardata;
    private int font_tex;
    private final STBTTAlignedQuad q;
    private final STBTTAlignedQuad q2;
    private final FloatBuffer      xb;
    private final FloatBuffer      yb;
    private final FloatBuffer      xb2;
    private final FloatBuffer      yb2;
    int rangeStart;
    int numChars;
    private STBTTFontinfo info;
    float ascent, descent, lineGap;
    private float drawedHeight;
    private float spaceWidth;
    private float lineOffset;
    private float size;
    public TrueTypeFontSTB(String fontPath, float fontSize, int style, boolean aa) {
        this.size = GameMath.round(fontSize);
        this.rangeStart = 32;
        this.numChars = 200;
        this.q = STBTTAlignedQuad.malloc();
        this.q2 = STBTTAlignedQuad.malloc();
        this.xb = memAllocFloat(1);
        this.yb = memAllocFloat(1);
        this.xb2 = memAllocFloat(1);
        this.yb2 = memAllocFloat(1);
        this.info = STBTTFontinfo.malloc();
        this.chardata = STBTTPackedchar.malloc(6 * 128);
        AssetBinary bin = AssetManager.getInstance().loadBin(fontPath);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(texW*texW);
        ByteBuffer ttf = (ByteBuffer)BufferUtils.createByteBuffer(bin.getData().length).put(bin.getData()).flip();
        int offset = stbtt_GetFontOffsetForIndex(ttf, 0);
        if (offset < 0) {
            throw new GameError("Failed loading "+fontPath);
        }
        int n = stbtt_InitFont(info, ttf, offset);
        if (n == 0) {
            throw new GameError("Failed loading "+fontPath);
        }
        int oversample = 2;
        float scale=stbtt_ScaleForPixelHeight(info, size);
        IntBuffer buf1=BufferUtils.createIntBuffer(1);
        IntBuffer buf2=BufferUtils.createIntBuffer(1);
        IntBuffer buf3=BufferUtils.createIntBuffer(1);
        stbtt_GetFontVMetrics(info, 
                (IntBuffer)buf1.position(0),
                (IntBuffer)buf2.position(0),
                (IntBuffer)buf3.position(0));
        this.ascent=scale*buf1.get(0);
        this.descent=scale*buf2.get(0);
        this.lineGap=scale*buf3.get(0);
        this.lineOffset = ascent - descent + lineGap;
        System.err.println(ascent +","+ descent +","+ lineGap);
        if (lineOffset == 0) {
            System.err.println(ascent +","+ descent +","+ lineGap);
            throw new GameError("Failed loading "+fontPath);
        }
        
        STBTTPackContext pc = STBTTPackContext.malloc();
        stbtt_PackBegin(pc, bitmap, texW, texW, 0, 1, null);
        stbtt_PackSetOversampling(pc, oversample, oversample);
        this.chardata.position(rangeStart);
        stbtt_PackFontRange(pc, ttf, 0, size, this.rangeStart, this.numChars, chardata);
        stbtt_PackEnd(pc);
        pc.free();
        chardata.position(0);
        
        byte[] data = new byte[texW*texW*4];
        for (int i = 0; i < texW*texW; i++) {
            byte b = bitmap.get(i);
            data[i*4+0] = (byte) 255;
            data[i*4+1] = (byte) 255;
            data[i*4+2] = (byte) 255;
            data[i*4+3] = b;
        }

        this.font_tex = TextureManager.getInstance().makeNewTexture(data, texW, texW, false, true, 0);

        this.spaceWidth = getCharWidth(' ');
    }

    @Override
    public float getCharWidth(int ch) {
        xb2.put(0, 0);
        yb2.put(0, 0);
        chardata.position(0);
        int idx = getIndex((char) ch);
        stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, 0);
        return xb2.get(0);
    }

    @Override
    public float getWidth(String text) {
        xb2.put(0, 0);
        yb2.put(0, 0);
        chardata.position(0);
        for (int i = 0; i < text.length(); i++) {
            char charCurrent = text.charAt(i);
            if (charCurrent == 0 && i + 1 < text.length()) {
                i++;
                int ctrl = getControlChar(charCurrent);
                if (ctrl >= 0) {
                    i++;
                    if (charCurrent == 'u' && i + 6 < text.length()) {
                        i += 6;
                    } else if (charCurrent == 's') {
                        xb2.put(0, xb2.get(0)+spaceWidth);
                    } else if (ctrl >= 0 && ctrl <= 15) {
                    }
                    continue;
                }
            }
            if (charCurrent == '\n')
                continue;
            int idx = getIndex(charCurrent);
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, 0);
        }
        return xb2.get(0);
    }

    @Override
    public float getCorrectL() {
        return 0;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void unallocate() {
        GL.deleteTexture(this.font_tex);
        info.free();
        memFree(chardata);
        memFree(yb);
        memFree(xb);
    }

    @Override
    public float getLineHeight() {
        return this.lineOffset;
    }

    @Override
    public int getTexture() {
        return this.font_tex;
    }

    @Override
    public float getCharHeight() {
        return getLineHeight()+descent;
    }

    @Override
    public float drawString(Tess tess, float x, float y, String text, int alignment, boolean shadow, float alpha, int maxWidth) {

        y-=2;
        final int startIndex = 0;
        final int endIndex = text.length() - 1;
        int charCurrent;
        float xPos = 0;
        float yPos = 0;
        int i = startIndex;
        

        switch (alignment) {
            case 3: {
                xPos = getWidth(text);
                xPos /= -2;
            }
            break;
            case ALIGN_RIGHT:
            case ALIGN_CENTER: {
                xPos = 0;
                int l = i;
                while (l <= endIndex) {
                    Character c2 = text.charAt(l);
                    if (c2 == '\n')
                        break;
                    l++;
                }
                String nextLine = text.substring(i, Math.min(text.length(), l));
                xPos=getWidth(nextLine);
                if (alignment != ALIGN_RIGHT) {
                    xPos /= -2;
                } else {
                    xPos = -xPos;
                }
            }
            case ALIGN_LEFT:
            default: {
                break;
            }
        }
        xb.put(0, xPos);
        yb.put(0, yPos);

        
        this.drawedHeight = this.lineOffset;
        while (i >= startIndex && i <= endIndex) {
            charCurrent = text.charAt(i);

            if (charCurrent == '\n') {
                yPos += this.lineOffset;
                this.drawedHeight += this.lineOffset;
                xPos = 0;
                if (alignment == ALIGN_CENTER || alignment == ALIGN_RIGHT) {
                    int l = i + 1;
                    while (l <= endIndex) {
                        Character c2 = text.charAt(l);
                        if (c2 == '\n')
                            break;
                        l++;
                    }
                    String nextLine = text.substring(i+1, Math.min(text.length(), l));
                    
                    xPos=getWidth(nextLine);
                    if (alignment != ALIGN_RIGHT)
                        xPos /= -2;
                    else {
                        xPos = -xPos;
                    }
                }
                xb.put(0, xPos);
                yb.put(0, yPos);
                i++;
                continue;
                // if center get next lines total width/2;
            }
            if (charCurrent == 0 && i + 1 < text.length()) {
                i++;
                charCurrent = text.charAt(i);
                int ctrl = getControlChar(charCurrent);
                if (ctrl >= 0) {
                    i++;
                    if (charCurrent == 'u' && i + 6 < text.length()) {
                        String hex = text.substring(i, i + 6);
                        int rgb = 0xFFFFFFFF;
                        try {
                            rgb = Integer.parseInt(hex, 16);//SLOW :(
                        } catch (NumberFormatException ignored) {
                        }
                        if (rgb >= 0 && rgb <= 0xFFFFFF) {
                            if (shadow) {
//                            OpenGlHelper.glColorIntRGB(0xFF000000|rgb);
                                tess.setColor(rgb, 0xFF);
                            }
                        }
                        i += 6;
                    } else if (charCurrent == 's') {
                        xPos += spaceWidth;
                        xb.put(0, xPos);
                    } else if (ctrl >= 0 && ctrl <= 15) {
                        if (!shadow) {
                            ctrl += 16;
                        }
                        final int l = colorMap[ctrl];
                        tess.setColorF(l, alpha);
                    }
                    continue;
                }
            }
            int idx = getIndex(charCurrent);
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb, yb, q, 0);
            xPos = xb.get(0);
            tess.add(x + q.x1(), y + q.y1(), 0F, q.s1(), q.t1());
            tess.add(x + q.x1(), y + q.y0(), 0F, q.s1(), q.t0());
            tess.add(x + q.x0(), y + q.y0(), 0F, q.s0(), q.t0());
            tess.add(x + q.x0(), y + q.y1(), 0F, q.s0(), q.t1());

            if (maxWidth > 0 && xPos >= maxWidth) break;
            i++;
        }
        return xb.get(0);
        
    }
    int getIndex(int charCurrent) {
        if (charCurrent < rangeStart || charCurrent > rangeStart+numChars) {
            return ' ';
        }
        return charCurrent;
    }
    @Override
    public float drawGlyph(float x, float y, int ch, boolean b, float alpha) {
        xb.put(0, 0);
        yb.put(0, 0);
        chardata.position(0);
        int idx = getIndex((char) ch);
        stbtt_GetPackedQuad(chardata, texW, texW, idx, xb, yb, q, 0);

        return xb.get(0);
    }

    @Override
    public float getLastDrawHeight() {
        return this.drawedHeight;
    }


    @Override
    public boolean hasCharacter(char c) {
        return c>=this.rangeStart&&c<=this.numChars;
    }

    public float getSpaceWidth() {
        return this.spaceWidth;
    }

}
