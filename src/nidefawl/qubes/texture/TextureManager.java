package nidefawl.qubes.texture;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import org.lwjgl.opengl.*;

public class TextureManager {
    final static TextureManager instance = new TextureManager();

    public int                  texNoise;
//    public int                  texNoise2;
    public int                  texEmpty;
    public int                  texEmptyNormal;
    private ByteBuffer          directBuf;
    

    TextureManager() {
    }

    public static TextureManager getInstance() {
        return instance;
    }


    public void init() {
        texNoise = glGenTextures();
//        texNoise2 = glGenTextures();
        reload();
        texEmpty = makeNewTexture(new byte[16*16*4], 16, 16, true, false, 0);
        int[] normalBumpMap = new int[16*16*4];
        Arrays.fill(normalBumpMap, 0xff7f7fff);
        byte[] ndata = TextureUtil.toBytesRGBA(normalBumpMap);
        texEmptyNormal = makeNewTexture(ndata, 16, 16, true, false, 0);
    }

    public void reload() {
        byte[] data = TextureUtil.genNoise(64);
        glBindTexture(GL_TEXTURE_2D, texNoise);
        TextureManager.getInstance().uploadTexture(data, 64, 64, 3, GL_RGB, GL_RGB, true, true, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        int size=256;
//        byte[] data2 = TextureUtil.genNoise2(size,size);
//        glBindTexture(GL_TEXTURE_2D, texNoise2);
//        TextureManager.getInstance().uploadTexture(data2, size, size, 3, GL_RGB, GL_RGB, true, true, 0);
//        glBindTexture(GL_TEXTURE_2D, 0);
    }


    public void load(String path) {

    }
    public int makeNewTexture(BufferedImage image, boolean repeat, boolean filter, int mipmapLevels) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("GL11.glBindTexture(GL11.GL_TEXTURE_2D, i)");

        byte[] data = TextureUtil.toBytesRGBA(image);
        uploadTexture(data, image.getWidth(), image.getHeight(), 4, GL11.GL_RGBA, GL11.GL_RGBA, repeat, filter, mipmapLevels);
        return i;
    }
    public int makeNewTexture(byte[] rgba, int w, int h, boolean repeat, boolean filter, int mipmapLevel) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        uploadTexture(rgba, w, h, 4, GL11.GL_RGBA, GL11.GL_RGBA, repeat, filter, mipmapLevel);
        return i;
    }
    public int makeNewTexture(AssetTexture tex, boolean repeat, boolean filter, int mipmapLevel) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        uploadTexture(tex.getData(), tex.getWidth(), tex.getHeight(), 4, GL11.GL_RGBA, GL11.GL_RGBA, repeat, filter, mipmapLevel);
        return i;
        
    }
    public int makeNewTexture(AssetTexture tex) {
        return makeNewTexture(tex, false, true, 0);
    }
    /**
     * Copy the supplied image onto the specified OpenGL texture
     */
    public void uploadTexture(byte[] rgba, int w, int h, int bytespp, int format, int internalFormat, boolean repeat, boolean filter, int mipmapLevel) {
        int magfilter = GL11.GL_NEAREST;
        int minfilter = GL11.GL_NEAREST;
        int wrap_s = GL12.GL_CLAMP_TO_EDGE;
        int wrap_t = GL12.GL_CLAMP_TO_EDGE;
        
        if (filter) {
            magfilter = GL11.GL_LINEAR;
            minfilter = GL11.GL_LINEAR;
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri GL_TEXTURE_MAX_LEVEL "+mipmapLevel);
        if (mipmapLevel > 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri GL_GENERATE_MIPMAP GL_TRUE");
//            GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, mipmapLevel, internalFormat, w, h);
            minfilter = filter ?  GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR_MIPMAP_NEAREST;
//            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magfilter);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri MAG_FILTER");
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minfilter);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri MIN_FILTER");
        if (repeat) {
            wrap_s = GL11.GL_REPEAT;
            wrap_t = GL11.GL_REPEAT;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap_s);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri WRAP_S ("+(repeat?"REPEAT":"CLAMP")+")");
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap_t);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri WRAP_T ("+(repeat?"REPEAT":"CLAMP")+")");
        if (directBuf == null || directBuf.capacity() < w*h*bytespp) {
            directBuf = ByteBuffer.allocateDirect(w*h*bytespp).order(ByteOrder.nativeOrder());
        }
        directBuf.clear();
        directBuf.put(rgba, 0, w*h*bytespp);
        directBuf.position(0).limit(w*h*bytespp);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, format, GL11.GL_UNSIGNED_BYTE, directBuf);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("GL11.glTexImage2D");
//        if (mipmapLevel > 0) {
//            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
//            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("GL30.glGenerateMipmap");
//        }
    }

    public int setupTexture(AssetTexture assetTexture, boolean repeat, boolean filter, int mipmapLvls) {
        return makeNewTexture(assetTexture.getData(), assetTexture.getWidth(), assetTexture.getHeight(), repeat, filter, mipmapLvls);
    }
    public void destroy() {
        // TODO Auto-generated method stub
        
    }
}
