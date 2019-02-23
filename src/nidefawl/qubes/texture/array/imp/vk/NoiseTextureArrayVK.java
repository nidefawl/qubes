package nidefawl.qubes.texture.array.imp.vk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.texture.TextureBinMips;

public class NoiseTextureArrayVK extends TextureArrayVK {
    
    public NoiseTextureArrayVK() {
        super(64);
    }

    @Override
    protected void uploadTextures() {
        int nBlock = 0;
        TextureBinMips[] binMips = new TextureBinMips[this.numTextures];
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            AssetTexture tex = entry.getValue().get(0);
            binMips[entry.getKey()] = new TextureBinMips(tex.getData(), this.tileSize, this.tileSize); 
            Engine.checkGLError("GL12.glTexSubImage3D");
            this.numUploaded++;
        }
        this.totalSlots = nBlock;
        this.texture.build(this.internalFormat, binMips);
    }

    @Override
    public void load() {
        super.load();
        this.numMipmaps = 1;
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
        collectNoiseTextures(mgr);
    }

}
