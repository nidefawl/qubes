package nidefawl.qubes.texture;

import nidefawl.qubes.texture.array.*;
import nidefawl.qubes.texture.array.impl.*;

/** Shortcut class to keep code small */
public class TMgr {

    public static int getNoise() {
        return TextureManager.getInstance().texNoise;
    }

    public static int getBlocks() {
        return TextureArrays.blockTextureArrayGL.glid;
    }
    
    public static int getNoiseArr() {
        return TextureArrays.noiseTextureArrayGL.glid;
    }

    public static int getItems() {
        return TextureArrays.itemTextureArrayGL.glid;
    }

    public static int getNormals() {
        return TextureArrays.blockNormalMapArrayGL.glid;
    }

    public static int getEmptyNormalMap() {
        return TextureManager.getInstance().texEmptyNormal;
    }

    public static int getEmpty() {
        return TextureManager.getInstance().texEmpty;
    }

    public static int getEmptyWhite() {
        return TextureManager.getInstance().texEmptyWhite;
    }

    public static int getEmptyUInt16RGBA() {
        return TextureManager.getInstance().texEmptyRGBA16UI;
    }

    public static int getEmptySpecularMap() {
        return TextureManager.getInstance().texEmpty;
    }
}
