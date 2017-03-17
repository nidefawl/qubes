package nidefawl.qubes.texture.array;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.texture.array.imp.vk.*;
import nidefawl.qubes.texture.array.impl.gl.*;

public class TextureArrays {
    public static TextureArray[] allArrays;
    public static TextureArray   blockTextureArray;
    public static TextureArray   blockNormalMapArray;
    public static TextureArray   itemTextureArray;
    public static TextureArray   noiseTextureArray;
    public static TextureArrayGL blockTextureArrayGL;
    public static TextureArrayGL blockNormalMapArrayGL;
    public static TextureArrayGL itemTextureArrayGL;
    public static TextureArrayGL noiseTextureArrayGL;
    public static TextureArrayVK blockTextureArrayVK;
    public static TextureArrayVK blockNormalMapArrayVK;
    public static TextureArrayVK itemTextureArrayVK;
    public static TextureArrayVK noiseTextureArrayVK;
    public static TextureArray[] init() {
        if (allArrays == null) {
            if (Engine.isVulkan) {
                blockTextureArray = blockTextureArrayVK = new BlockTextureArrayVK();
                blockNormalMapArray = blockNormalMapArrayVK = new BlockNormalMapArrayVK();
                itemTextureArray = itemTextureArrayVK = new ItemTextureArrayVK();
                noiseTextureArray = noiseTextureArrayVK = new NoiseTextureArrayVK();
            } else {
                blockTextureArray = blockTextureArrayGL = new BlockTextureArrayGL();
                blockNormalMapArray = blockNormalMapArrayGL = new BlockNormalMapArrayGL();
                itemTextureArray = itemTextureArrayGL = new ItemTextureArrayGL();
                noiseTextureArray = noiseTextureArrayGL = new NoiseTextureArrayGL();
            }
            allArrays = new TextureArray[] {
                    blockTextureArray,
                    blockNormalMapArray,
                    itemTextureArray,
                    noiseTextureArray
            };
        }
        return allArrays;
    }
    


}
