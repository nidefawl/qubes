package nidefawl.qubes.texture;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nidefawl.game.Main;
import nidefawl.qubes.gl.Engine;

import org.lwjgl.opengl.*;

public class TextureManager {
    final static TextureManager instance = new TextureManager();

    TextureManager() {
    }

    public static TextureManager getInstance() {
        return instance;
    }

    public void init() {
    }

    private ByteBuffer directBuf;

    public void load(String path) {

    }
    public int makeNewTexture(BufferedImage image, boolean repeat, boolean filter, int mipmapLevels) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        byte[] data = getRGBA(image);
        uploadTexture(data, image.getWidth(), image.getHeight(), 4, GL11.GL_RGBA, GL11.GL_RGBA, repeat, filter, mipmapLevels);
        return i;
    }
    public int makeNewTexture(byte[] rgba, int w, int h, boolean repeat, boolean filter, int mipmapLevel) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        uploadTexture(rgba, w, h, 4, GL11.GL_RGBA, GL11.GL_RGBA, repeat, filter, mipmapLevel);
        return i;
    }
    public static byte[] getRGBA(int[] irgba) {
        byte textureData[] = new byte[irgba.length * 4];
        for (int k1 = 0; k1 < irgba.length; k1++) {
            if (irgba[k1] >> 24 == 0) {
                for (int a = 0; a < 4; a++) 
                    textureData[k1 * 4 + a] = 0;
            } else {
                textureData[k1 * 4 + 0] = (byte)(irgba[k1] >> 16 & 0xff);
                textureData[k1 * 4 + 1] = (byte)(irgba[k1] >> 8 & 0xff);
                textureData[k1 * 4 + 2] = (byte)(irgba[k1] >> 0 & 0xff);
                textureData[k1 * 4 + 3] = (byte)(irgba[k1] >> 24 & 0xff);
            }
        }
        return textureData;
    }
    public static byte[] getRGBA(BufferedImage bufferedImage) {
        int j = bufferedImage.getWidth();
        int l = bufferedImage.getHeight();
        int rgba[] = new int[j * l];
        bufferedImage.getRGB(0, 0, j, l, rgba, 0, j);
        byte textureData[] = new byte[rgba.length * 4];
        for (int k1 = 0; k1 < rgba.length; k1++) {
            if (rgba[k1] >> 24 == 0) {
                for (int a = 0; a < 4; a++) 
                    textureData[k1 * 4 + a] = 0;
            } else {
                textureData[k1 * 4 + 0] = (byte)(rgba[k1] >> 16 & 0xff);
                textureData[k1 * 4 + 1] = (byte)(rgba[k1] >> 8 & 0xff);
                textureData[k1 * 4 + 2] = (byte)(rgba[k1] >> 0 & 0xff);
                textureData[k1 * 4 + 3] = (byte)(rgba[k1] >> 24 & 0xff);
            }
        }
        return textureData;
    }

    /**
     * Copy the supplied image onto the specified OpenGL texture
     */
    public void uploadTexture(byte[] rgba, int w, int h, int bytespp, int format, int internalFormat, boolean repeat, boolean filter, int mipmapLevel) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        int magfilter = GL11.GL_NEAREST;
        int minfilter = GL11.GL_NEAREST;
        int wrap_s = GL11.GL_CLAMP;
        int wrap_t = GL11.GL_CLAMP;
        
        if (filter) {
            magfilter = GL11.GL_LINEAR;
            minfilter = GL11.GL_LINEAR;
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri GL_TEXTURE_MAX_LEVEL "+mipmapLevel);
        if (mipmapLevel > 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri GL_GENERATE_MIPMAP GL_TRUE");
//            GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, mipmapLevel, internalFormat, w, h);
            minfilter = filter ?  GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR_MIPMAP_NEAREST;
//            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magfilter);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri MAG_FILTER");
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minfilter);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri MIN_FILTER");
        if (repeat) {
            wrap_s = GL11.GL_REPEAT;
            wrap_t = GL11.GL_REPEAT;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap_s);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri WRAP_S");
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap_t);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri WRAP_T");
        if (directBuf == null || directBuf.capacity() < w*h*bytespp) {
            directBuf = ByteBuffer.allocateDirect(w*h*bytespp).order(ByteOrder.nativeOrder());
        }
        directBuf.clear();
        directBuf.put(rgba, 0, w*h*bytespp);
        directBuf.position(0).limit(w*h*bytespp);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, format, GL11.GL_UNSIGNED_BYTE, directBuf);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("GL11.glTexImage2D");
//        if (mipmapLevel > 0) {
//            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
//            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("GL30.glGenerateMipmap");
//        }
    }

    public void destroy() {
        // TODO Auto-generated method stub
        
    }
}
