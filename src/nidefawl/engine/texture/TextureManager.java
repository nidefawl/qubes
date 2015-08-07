package nidefawl.engine.texture;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL11;

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
    public int makeNewTexture(BufferedImage image, boolean repeat, boolean filter) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        byte[] data = getRGBA(image);
        uploadTexture(data, image.getWidth(), image.getHeight(), repeat, filter);
        return i;
    }
    public int makeNewTexture(byte[] rgba, int w, int h, boolean repeat, boolean filter) {
        int i = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, i);
        uploadTexture(rgba, w, h, repeat, filter);
        return i;
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
    public void uploadTexture(byte[] rgba, int w, int h, boolean repeat, boolean filter) {
        if (filter) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);   
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
        if (repeat) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        }
        if (directBuf == null || directBuf.capacity() < w*h*4) {
            directBuf = ByteBuffer.allocateDirect(w*h*4).order(ByteOrder.nativeOrder());
        }
        directBuf.clear();
        directBuf.put(rgba, 0, w*h*4);
        directBuf.position(0).limit(w*h*4);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, directBuf);
    }

    public void destroy() {
        // TODO Auto-generated method stub
        
    }
}
