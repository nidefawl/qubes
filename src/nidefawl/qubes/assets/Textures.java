package nidefawl.qubes.assets;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;

import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;

import org.lwjgl.BufferUtils;

public class Textures {
    public static int texNoise;
    public static int texEmpty;
    public static AssetTexture texWater;
    public static AssetTexture texWater2;

    public void genNoise() {
        int w = 64;
        byte[] data = new byte[w*w*3];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                for (int y = 0; y < 3; y++) {
                    int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                    data[(x*64+z)*3+y]=(byte) (GameMath.randomI(seed)%128);
                }
        glBindTexture(GL_TEXTURE_2D, texNoise);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); 
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        ByteBuffer buf = BufferUtils.createByteBuffer(data.length);
        buf.put(data);
        buf.position(0).limit(data.length);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, w, 0, GL_RGB, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void init() {
        texNoise = glGenTextures();
        genNoise();
        texEmpty = TextureManager.getInstance().makeNewTexture(new byte[16*16*4], 16, 16, true, false);
        texWater = AssetManager.getInstance().loadPNGAsset("textures/water.png");
        texWater.setupTexture();
        texWater2 = AssetManager.getInstance().loadPNGAsset("textures/water_still.png");
        texWater2.setupTexture();
    }

}
