package nidefawl.qubes.texture;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.texture.TextureCreateInfo.FilterType;
import nidefawl.qubes.texture.TextureCreateInfo.TextureSub;
import nidefawl.qubes.texture.TextureCreateInfo.UVCoordMode;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class TextureManager {
    final static TextureManager instance = new TextureManager();

    public int                  texNoise;
//    public int                  texNoise2;
    public int                  texEmpty;
    public int                  texEmptyNormal;
    private ByteBuffer          directBuf;

    public int texEmptyWhite;

    public int texEmptyRGBA16UI;
    

    TextureManager() {
    }

    public static TextureManager getInstance() {
        return instance;
    }


    public void init() {
        texNoise = glGenTextures();
//        texNoise2 = glGenTextures();
        reload();
        byte[] emptyTexData = new byte[16*16*4];
        texEmpty = makeNewTexture(emptyTexData, 16, 16, true, false, 0, GL11.GL_RGBA);
        Arrays.fill(emptyTexData, (byte)0xff);
        texEmptyWhite = makeNewTexture(emptyTexData, 16, 16, true, false, 0, GL11.GL_RGBA);
        int[] normalBumpMap = new int[16*16*4];
        Arrays.fill(normalBumpMap, 0xff7f7fff);
        byte[] ndata = TextureUtil.toBytesRGBA(normalBumpMap);
        texEmptyNormal = makeNewTexture(ndata, 16, 16, true, false, 0, GL11.GL_RGBA);
        texEmptyRGBA16UI = glGenTextures();
        int ns = 0;
        glBindTexture(GL_TEXTURE_2D, texEmptyRGBA16UI);
        GL.glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA16UI, 1, 1);
        Engine.checkGLError("setup empty uint16 rgba "+(ns++));
        GL11.glTexParameteri(GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        directBuf.clear();
        directBuf.put(new byte[4*1*1*2]);
        directBuf.position(0).limit(4*1*1*2);
        GL11.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 1, 1, GL30.GL_BGRA_INTEGER, GL11.GL_UNSIGNED_BYTE, directBuf);
        Engine.checkGLError("setup empty uint16 rgba "+(ns++));
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void reload() {
        int size=256;
        byte[] data = TextureUtil.genNoise2(size);
        glBindTexture(GL_TEXTURE_2D, texNoise);
        TextureManager.getInstance().uploadTexture(data, size, size, 3, GL_RGB8, GL_RGB, true, true, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
//        byte[] data2 = TextureUtil.genNoise2(size,size);
//        glBindTexture(GL_TEXTURE_2D, texNoise2);
//        TextureManager.getInstance().uploadTexture(data2, size, size, 3, GL_RGB, GL_RGB, true, true, 0);
//        glBindTexture(GL_TEXTURE_2D, 0);
    }


    public void load(String path) {

    }
    public int makeDDS2DTexture(AssetTexture tex, boolean repeat, boolean filter) {
        DDSLoader dds = tex.getDds();
        if (dds == null) {
            throw new GameError("Not yet implemented, please load PNGs manually");
        }
        TextureCreateInfo info = new TextureCreateInfo(dds);
        info.setFilter(filter?FilterType.LINEAR:FilterType.NEAREST);
        info.setUVMode(repeat?UVCoordMode.REPEAT:UVCoordMode.CLAMP);
        return makeCompressedTexture(info);
    }
    public int makeCompressedTexture(TextureCreateInfo ti) {
        int internalFormat = ti.getGLFormat();
        boolean isCompressedFormat = ti.isCompresedFormat();
        int mipmapLevel = ti.getNumMips()-1;
        ByteBuffer data = ti.getData();
        int magfilter = GL11.GL_NEAREST;
        int minfilter = GL11.GL_NEAREST;
        int wrap_s = GL12.GL_CLAMP_TO_EDGE;
        int wrap_t = GL12.GL_CLAMP_TO_EDGE;
        boolean filter = ti.getFilter() == FilterType.LINEAR;
        boolean repeat = ti.getUVMode() == UVCoordMode.REPEAT;
        if (filter) {
            magfilter = GL11.GL_LINEAR;
            minfilter = GL11.GL_LINEAR;
        }
        TextureSub baseLevel = ti.getLevel(0);
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        nidefawl.qubes.gl.GL.glTexStorage2D(GL11.GL_TEXTURE_2D, mipmapLevel+1, internalFormat, baseLevel.getWidth(), baseLevel.getHeight());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameteri GL_TEXTURE_MAX_LEVEL "+mipmapLevel);
        if (mipmapLevel > 0) {
            minfilter = filter ?  GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR_MIPMAP_NEAREST;
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


        Engine.checkGLError("glGetTexLevelParameteri");    
        System.out.println("new internal "+internalFormat);

        int offset = 0;
        for (int n = 0; n <= mipmapLevel; n++) {
            TextureSub mipLevel = ti.getLevel(n);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexImage2D");    

            int size = mipLevel.getSize();
            System.out.println("size "+size);
            data.position(offset).limit(offset+size);
            if (directBuf == null || directBuf.capacity() < size) {
                directBuf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
            }
            directBuf.clear();
            directBuf.put(data);
            directBuf.position(0).limit(size);

            if (isCompressedFormat) {
                GL13.glCompressedTexSubImage2D(GL11.GL_TEXTURE_2D, n, 0, 0, mipLevel.getWidth(), mipLevel.getHeight(), internalFormat, directBuf);
                if (Game.GL_ERROR_CHECKS) Engine.checkGLError("dds  glCompressedTexSubImage2D level "+n+" format "+internalFormat+", data "+size+", buffer "+directBuf+", dimensions "+mipLevel.getWidth()+"x"+mipLevel.getHeight());
            } else {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, n, mipLevel.getWidth(), mipLevel.getHeight(), 0, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, directBuf);
                if (Game.GL_ERROR_CHECKS) Engine.checkGLError("dds glTexSubImage2D");    
            }
            offset += size;
        }
        if (filter) {
            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexParameterf GL_TEXTURE_MAX_ANISOTROPY_EXT");
        }
        float anisotropicFilterLevel = ti.getAnisotropicFilterLevel();
        if (anisotropicFilterLevel > 0) {

            float f = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            if (anisotropicFilterLevel < f) {
                f = anisotropicFilterLevel;
            }
            if (f > 0) {
                System.out.println("GL_TEXTURE_MAX_ANISOTROPY_EXT "+f);
                glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, f);
            }
        }
        return i;
    }

    public int makeNewTexture(byte[] rgba, int w, int h, boolean repeat, boolean filter, int mipmapLevel, int format) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        int fmt = GL11.GL_RGBA;
        int ifmt = format;
        int bytespp = 4;
        if (w*h*4 > rgba.length) {
            if (rgba.length<w*h) {
                throw new GameError("Cannot generate textures, date seems to be non of [RGBA, R]");
            }
            ifmt = GL_R8;
            fmt = GL_RED;
            bytespp = 1;
        }
        uploadTexture(rgba, w, h, bytespp, fmt, ifmt, repeat, filter, mipmapLevel);
        Engine.checkGLError("uploadTexture");
        return i;
    }
    
    public int makeNewTexture(AssetTexture tex, boolean repeat, boolean filter, int mipmapLevel) {
        return makeNewTexture(tex.getData(), tex.getWidth(), tex.getHeight(), repeat, filter, mipmapLevel, GL11.GL_RGBA);
    }
    public int makeNewTexture(AssetTexture tex, boolean repeat, boolean filter, int mipmapLevel, int format) {
        return makeNewTexture(tex.getData(), tex.getWidth(), tex.getHeight(), repeat, filter, mipmapLevel, format);
    }
    
    public int makeNewTexture(AssetTexture tex) {
        return makeNewTexture(tex, false, true, 0);
    }
    /**
     * Copy the supplied image onto the specified OpenGL texture
     */
    public void uploadTexture(byte[] rgba, int w, int h, int bytespp, int format, int internalFormat, boolean repeat, boolean filter, int mipmapLevel) {
        if (mipmapLevel < 0) {
            int maxDim = Math.max(w, h);
            mipmapLevel = 1+GameMath.log2(maxDim);
        }
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
//            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
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
        if (mipmapLevel > 0) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);
            if (GameBase.GL_ERROR_CHECKS) Engine.checkGLError("GL30.glGenerateMipmap");
        }
    }

    public void destroy() {
        
    }
}
