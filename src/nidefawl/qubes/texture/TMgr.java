package nidefawl.qubes.texture;

/** Shortcut class to keep code small */
public class TMgr {

    public static int getNoise() {
        return TextureManager.getInstance().texNoise;
    }

    public static int getBlocks() {
        return BlockTextureArray.getInstance().glid;
    }

    public static int getItems() {
        return ItemTextureArray.getInstance().glid_color;
    }

    public static int getEmptyNormalMap() {
        return TextureManager.getInstance().texEmptyNormal;
    }

    public static int getEmpty() {
        return TextureManager.getInstance().texEmpty;
    }

    public static int getEmptySpecularMap() {
        return TextureManager.getInstance().texEmpty;
    }

    public static int getNormals() {
        return BlockNormalMapArray.getInstance().glid;
    }
    public static int getItemNormals() {
        return ItemTextureArray.getInstance().glid_normalmaps;
    }
}
