package nidefawl.qubes.font;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.stb.*;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vulkan.*;

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
    public final static String ctrls = "0123456789abcdefsu";

    public static int getControlChar(int charCurrent) {
        if (charCurrent==0) {
            return 255;
        }
        return ctrls.indexOf(charCurrent);
    }

    public String trimColorChars(final String in) {
        return in.replaceAll("(\u00A7([a-f0-9]))", "");
    }
    
    int                            texW     = 512;
    public int                     correctL = 1;
    private Buffer                 chardata;
    private int                    font_tex;
    VkTexture                      vk_tex;
    private final STBTTAlignedQuad q;
    private final STBTTAlignedQuad q2;
    private final FloatBuffer      xb;
    private final FloatBuffer      yb;
    private final FloatBuffer      xb2;
    private final FloatBuffer      yb2;
    int                            rangeStart;
    int                            numChars;
    private STBTTFontinfo          info;
    float                          ascent, descent, lineGap;
    private float                  drawedHeight;
    private float                  spaceWidth;
    private float                  lineOffset;
    private float                  size;
    public TextureBinMips         binMips;
    public VkDescriptor descriptorSetTex;
    
    public TrueTypeFont(String fontPath, float fontSize, int style, boolean aa) {
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
        this.chardata = STBTTPackedchar.malloc(this.numChars);
        AssetBinary bin = AssetManager.getInstance().loadBin(fontPath);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(texW*texW);
        ByteBuffer ttf = (ByteBuffer)BufferUtils.createByteBuffer(bin.getData().length).put(bin.getData()).flip();
        int offset = stbtt_GetFontOffsetForIndex(ttf, 0);
        if (offset < 0) {
            throw new GameError("Failed loading "+fontPath);
        }
        if (true!=stbtt_InitFont(info, ttf, offset)) {
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
        if (lineOffset == 0) {
            System.err.println(ascent +","+ descent +","+ lineGap);
            throw new GameError("Failed loading "+fontPath);
        }
        
        STBTTPackContext pc = STBTTPackContext.malloc();
        stbtt_PackBegin(pc, bitmap, texW, texW, 0, 1);
        stbtt_PackSetOversampling(pc, oversample, oversample);
        this.chardata.position(rangeStart);
        stbtt_PackFontRange(pc, ttf, 0, size, this.rangeStart, chardata);
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
        if (!Engine.isVulkan) {
            this.font_tex = TextureManager.getInstance().makeNewTexture(data, texW, texW, false, true, 0, GL11.GL_RGBA);
        } else {
            this.binMips = new TextureBinMips(data, texW, texW);
            Engine.registerTTF(this);
        }

        this.spaceWidth = getCharWidth(' ');
    }


    public float getCharWidth(int ch) {
        xb2.put(0, 0);
        yb2.put(0, 0);
        
        int idx = getIndex((char) ch);
        stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, false);
        return xb2.get(0);
    }


    public float getWidthAtLine(String text) {
        xb2.put(0, 0);
        yb2.put(0, 0);
        float lastw = 0;
        int nLine = 0;
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
            if (charCurrent == '\n') {
                lastw = xb2.get(0);
                nLine++;
                xb2.put(0, 0);
                continue;
            }
            int idx = getIndex(charCurrent);
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, false);
        }
        return xb2.get(0);
    }
    public float getWidth(String text) {
        xb2.put(0, 0);
        yb2.put(0, 0);
        
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
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, false);
        }
        return xb2.get(0);
    }
    public void start(float x, float y) {
        xb.put(0, x);
        yb.put(0, y);
    }
    public float getXPos() {
        return xb.get(0);
    }
    public float getYPos() {
        return yb.get(0);
    }
    public void readQuad(int charCurrent) {
        int idx = getIndex(charCurrent);
        stbtt_GetPackedQuad(chardata, texW, texW, idx, xb, yb, q, false);
    }
    public void renderQuad(ITess tessellator, float x, float y) {
        tessellator.add(x + q.x1(), y + q.y1(), 0F, q.s1(), q.t1());
        tessellator.add(x + q.x1(), y + q.y0(), 0F, q.s1(), q.t0());
        tessellator.add(x + q.x0(), y + q.y0(), 0F, q.s0(), q.t0());
        tessellator.add(x + q.x0(), y + q.y1(), 0F, q.s0(), q.t1());
    }

    public int getCharPositionFromXCoord(String editText, double mouseX, float shiftPX) {
        xb2.put(0, -shiftPX);
        yb2.put(0, 0);
        int mX = (int) Math.round(mouseX);
        for (int i = 0; i < editText.length(); i++) {
            char charCurrent = editText.charAt(i);
            if (charCurrent == 0 && i + 1 < editText.length()) {
                i++;
                int ctrl = getControlChar(charCurrent);
                if (ctrl >= 0) {
                    i++;
                    if (charCurrent == 'u' && i + 6 < editText.length()) {
                        i += 6;
                    } else if (charCurrent == 's') {
                        xb2.put(0, xb2.get(0)+this.spaceWidth);
                    } else if (ctrl >= 0 && ctrl <= 15) {
                    }
                    continue;
                }
            }
            int idx = getIndex(charCurrent);
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb2, yb2, q2, false);
            if (xb2.get(0) >= mX) {
                return Math.max(0, i);
            }
        }
        return editText.length();
    }

    public float getCorrectL() {
        return 0;
    }


    public boolean isValid() {
        return true;
    }


    public void release() {
        if (!Engine.isVulkan) {
            GL.deleteTexture(this.font_tex);
        }
        this.info.free();
        this.q.free();
        this.q2.free();
//        memFree(this.chardata);
        this.chardata.free();
        this.chardata = null;
        memFree(this.yb);
        memFree(this.xb);
        memFree(this.yb2);
        memFree(this.xb2);
    }


    public float getLineHeight() {
        return this.lineOffset;
    }


    public int getTexture() {
        return this.font_tex;
    }


    public float getCharHeight() {
        return getLineHeight()+descent;
    }


    public float drawString(ITess tess, float x, float y, String text, int alignment, boolean shadow, float alpha, int maxWidth, int baseColor, float baseAlpha) {

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
                    } else if (ctrl == 255) {
                        tess.setColorF(baseColor, baseAlpha);
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
            stbtt_GetPackedQuad(chardata, texW, texW, idx, xb, yb, q, false);
            xPos = xb.get(0);
//            if (Engine.isVulkan || Engine.OGL_INVERSE_Y) {
//                float x1 = q.s1();
//                float x2 = q.s0();
//                float y1 = q.t1();
//                float y2 = q.t0();
//                tess.add(x + q.x1(), y +lineGap - q.y1(), 0F, x1, y1);
//                tess.add(x + q.x1(), y +lineGap - q.y0(), 0F, x1, y2);
//                tess.add(x + q.x0(), y +lineGap - q.y0(), 0F, x2, y2);
//                tess.add(x + q.x0(), y +lineGap - q.y1(), 0F, x2, y1);
//            } else {
                float x1 = q.s1();
                float x2 = q.s0();
                float y1 = q.t1();
                float y2 = q.t0();
                tess.add(x + q.x1(), y + q.y1(), 0F, x1, y1);
                tess.add(x + q.x1(), y + q.y0(), 0F, x1, y2);
                tess.add(x + q.x0(), y + q.y0(), 0F, x2, y2);
                tess.add(x + q.x0(), y + q.y1(), 0F, x2, y1);
//            }

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

    public float drawGlyph(float x, float y, int ch, boolean b, float alpha) {
        xb.put(0, 0);
        yb.put(0, 0);
        
        int idx = getIndex((char) ch);
        stbtt_GetPackedQuad(chardata, texW, texW, idx, xb, yb, q, false);

        return xb.get(0);
    }


    public float getLastDrawHeight() {
        return this.drawedHeight;
    }



    public boolean hasCharacter(char c) {
        return c>=this.rangeStart&&c<=this.numChars;
    }

    public float getSpaceWidth() {
        return this.spaceWidth;
    }

    public void drawTextBuffer(ITess tess) {
        if (!Engine.isVulkan) {
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, getTexture());
        } else {
            if (this.vk_tex == null) {
                return;
            }
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descriptorSetTex);
        }
        Engine.setPipeStateFontrenderer();
        tess.drawQuads();
    }

    public void bindTexture() {
        if (!Engine.isVulkan) {
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, getTexture());
        } else {
            if (this.vk_tex == null) {
                return;
            }
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descriptorSetTex);
        }
    }

    public void setup(VKContext vkContext) {
        int vkFormat = VK_FORMAT_R8G8B8A8_UNORM;
        try ( MemoryStack stack = stackPush() ) {

            this.vk_tex = new VkTexture(vkContext);
            this.vk_tex.build(vkFormat, this.binMips);
            this.vk_tex.genView();
        
//          vkContext.descLayouts.getDescriptorSets);
            this.descriptorSetTex = vkContext.descLayouts.allocDescSetSampleSingle();
            
            this.descriptorSetTex.setBindingCombinedImageSampler(0, this.vk_tex.getView(), vkContext.samplerLinear, this.vk_tex.getImageLayout());
            this.descriptorSetTex.update(vkContext);
        }
    }

    public void destroy(VKContext vkContext) {
        if (this.vk_tex != null) {
            this.vk_tex.destroy();
            this.vk_tex = null;
        }
    }
}
