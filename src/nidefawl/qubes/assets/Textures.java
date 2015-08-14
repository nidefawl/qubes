package nidefawl.qubes.assets;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import nidefawl.game.Main;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.SimplexValueNoise;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TimingHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public class Textures {
    public static int texNoise;
    public static int texNoise2;
    public static int texEmpty;
    public static AssetTexture texWater;
    public static AssetTexture texWater2;
    public static AssetTexture texStone;
    public static int texEmptyNormal;
    public static int blockTextureMap;

    public void genNoise(int texture) {
        int w = 64;
        byte[] data = new byte[w*w*3];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                for (int y = 0; y < 3; y++) {
                    int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                    data[(x*64+z)*3+y]=(byte) (GameMath.randomI(seed)%128);
                }

        glBindTexture(GL_TEXTURE_2D, texture);
        TextureManager.getInstance().uploadTexture(data, w, w, 3, GL_RGB, GL_RGB, true, true, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    public void genNoise2(int texture) {
        int noct = 8;
        long seed = 0xdeadbeefL;
        seed--;
        SimplexValueNoise n1 = new SimplexValueNoise(seed);
        OpenSimplexNoise n2 = new OpenSimplexNoise(seed);
        OpenSimplexNoise n3 = new OpenSimplexNoise(seed*19);
        int w = Main.displayWidth;
        int h = Main.displayHeight;
        if (h%2 != 0)
            h++;
        if (w%2!=0)
            w++;
        byte[] data = new byte[w*h*3];
        TimingHelper.startSilent(123);
        float f1=Main.ticksran+Main.instance.timer.partialTick;
        int iW = a;
        double scale = 1/10D;
        double scale2 = scale*5;
        double[] dNoise = new double[(w+iW*2)*(h+iW*2)];
        for (int iX = -iW; iX < w+iW; iX++)
            for (int iZ = -iW; iZ < h+iW; iZ++) {
              double d1 = n2.eval(iX*scale2, iZ*scale2)*0.1D;
              double d2 = n3.eval(iX*scale2, iZ*scale2)*0.1D;
              
                double d = -2D;
                double dX = (iX)*scale;
                double dZ = (iZ)*scale;
                double dN = n1.eval(dX+d1, dZ+d2);
                dN *= 0.5D;
                dN += 0.5D;
                d += (dN*7);
                d = Math.min(1, d);
                d = Math.max(0, d);
                d = 1 - d;
                d+=0.08D;
                d = Math.pow(d, 12);
//                d-=1D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                dNoise[((iZ+iW)*(w+iW*2))+(iX+iW)] = d;
            }

        for (int ix = 0; ix < w; ix++)
            for (int iz = 0; iz < h; iz++) {
                double d = getBlur(dNoise, ix, iz, iW, iW, w);
                d = 1-d;
                d = Math.pow(d, 4);
//              d-=1D;
              d = Math.min(1, d);
              d = Math.max(0, d);
              d = 1-d;
                int lum = (int) (d*255);
//              int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                for (int y = 0; y < 3; y++) {
                    data[(iz*w+ix)*3+y]= (byte) lum;
                }
//                break;
            }
        glBindTexture(GL_TEXTURE_2D, texture);
        TextureManager.getInstance().uploadTexture(data, w, h, 3, GL_RGB, GL_RGB, true, true, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        long l = TimingHelper.stopSilent(123);
        System.out.println(l);
    }
    int a = 0;
    
    public void genNoise3(int texture) {
        int noct = 8;
        long seed = 0xdeadbeefL;
        seed--;
        OpenSimplexNoise n1 = new OpenSimplexNoise(seed);
        OpenSimplexNoise n2 = new OpenSimplexNoise(seed*43);
        OpenSimplexNoise n3 = new OpenSimplexNoise(seed*19);
        int w = Main.displayWidth;
        int h = Main.displayHeight;
        if (h%2 != 0)
            h++;
        if (w%2!=0)
            w++;
        byte[] data = new byte[w*h*3];
        float f1=Main.ticksran+Main.instance.timer.partialTick;
        TimingHelper.startSilent(123);
        double scale = 1/50D;
        double scale2 = scale*5;
//        a = (++a)%6;
        a = 2;
        int iW = a;
        double[] dNoise = new double[(w+iW*2)*(h+iW*2)];
        for (int iX = -iW; iX < w+iW; iX++)
            for (int iZ = -iW; iZ < h+iW; iZ++) {
              double d1 = n2.eval(iX*scale2, iZ*scale2)*0.1D;
              double d2 = n3.eval(iX*scale2, iZ*scale2)*0.1D;
              
                double d = -1D;
                double dX = (iX)*scale;
                double dZ = (iZ)*scale;
                double dN = n1.eval(dX+d1, dZ+d2);
                d += Math.abs(dN)*10;
                d *= 0.5D;
                d += 0.5D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                d = 1 - d;
                d+=0.08D;
                d = Math.pow(d, 12);
//                d-=1D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                dNoise[((iZ+iW)*(w+iW*2))+(iX+iW)] = d;
            }

        for (int ix = 0; ix < w; ix++)
            for (int iz = 0; iz < h; iz++) {
                double d = getBlur(dNoise, ix, iz, iW, iW, w);
                d = 1-d;
                d = Math.pow(d, 4);
//              d-=1D;
              d = Math.min(1, d);
              d = Math.max(0, d);
              d = 1-d;
                int lum = (int) (d*255);
//              int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                for (int y = 0; y < 3; y++) {
                    data[(iz*w+ix)*3+y]= (byte) lum;
                }
//                break;
            }
        glBindTexture(GL_TEXTURE_2D, texture);
        TextureManager.getInstance().uploadTexture(data, w, h, 3, GL_RGB, GL_RGB, true, true, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        long l = TimingHelper.stopSilent(123);
        System.out.println(l);
    }

    private double getBlur(double[] dNoise, int x, int z, int iW, int i, int w) {
        if (i == 0) {
            return dNoise[((z+iW)*(w+iW*2))+(x+iW)]; 
        }
        double d = 0;
        double[] dWeights = new double[i*i*2*2];
        double dmax = 0D;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                double dD = GameMath.dist2d(0, 0, x1, z1);
                if (dD > dmax) dmax = dD;
                dWeights[((z1+i)*(i*2))+(x1+i)] = dD;
            }
        }
        for (int j = 0; j < dWeights.length; j++) {
            dWeights[j] = 1.0D - dWeights[j]/dmax;
        }
        double weight = 0;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                int xx = x1+x;
                int zz = z1+z;
                double dWeight = dWeights[((z1+i)*(i*2))+(x1+i)];
                weight += dWeight;
//                if (x1==-i&&z1==-i)
//                System.out.printf("%d, %d = %.2f\n", x1, z1, dWeight);
                d+=dNoise[((zz+iW)*(w+iW*2))+(xx+iW)]*dWeight;
            }
        }
        return d/weight;
    }

    public void init() {
        ByteBuffer directBuf = null;
        texNoise = glGenTextures();
        texNoise2 = glGenTextures();
        genNoise(texNoise);
        genNoise2(texNoise2);
        texEmpty = TextureManager.getInstance().makeNewTexture(new byte[16*16*4], 16, 16, true, false, 0);
        int[] normalBumpMap = new int[16*16*4];
        Arrays.fill(normalBumpMap, 0xff7f7fff);
        byte[] ndata = TextureManager.getRGBA(normalBumpMap);
        texEmptyNormal = TextureManager.getInstance().makeNewTexture(ndata, 16, 16, true, false, 0);
        texWater = AssetManager.getInstance().loadPNGAsset("textures/water.png");
        texWater.setupTexture(true, false, 4);
        texWater2 = AssetManager.getInstance().loadPNGAsset("textures/water_still.png");
        texWater2.setupTexture(true, false, 4);
        texStone = AssetManager.getInstance().loadPNGAsset("textures/stone2.png");
        texStone.setupTexture(true, false, 4);
        int text = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, text);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");
        //Create storage for the texture. (100 layers of 1x1 texels)
        String[] textures = new String[] {
                "textures/stone.png",
                "textures/dirt.png",
                "textures/grass.png",
                "textures/sand.png",
                "textures/water.png",
        };
        int tileSize = texStone.getWidth();
        GL42.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, 
                1,                    //No mipmaps as textures are 1x1
                GL_RGBA8,              //Internal format
                tileSize, tileSize,   //width,height
                textures.length       //Number of layers
        );
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("GL42.glTexStorage3D");
        for (int i = 0; i < textures.length; i++) {
            AssetTexture tex = AssetManager.getInstance().loadPNGAsset(textures[i]);
            byte[] data = tex.getData();
            if (directBuf == null || directBuf.capacity() < data.length) {
                directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
            }
            directBuf.clear();
            directBuf.put(data, 0, data.length);
            directBuf.position(0).limit(data.length);
            GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 
                    0,                     //Mipmap number
                    0, 0, i,                 //xoffset, yoffset, zoffset
                    tileSize, tileSize, 1,                 //width, height, depth
                    GL_RGBA,                //format
                    GL_UNSIGNED_BYTE,      //type
                    directBuf);                //pointer to data
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("GL12.glTexSubImage3D");

        }
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        blockTextureMap = text;
    }

    public void refreshNoiseTextures() {
        genNoise(texNoise);
        genNoise2(texNoise2);
    }

}
