package nidefawl.qubes.texture.array.imp.vk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

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
        int totalTex = this.blockIDToAssetList.size();
        TextureBinMips[] binMips = new TextureBinMips[this.numTextures];
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            AssetTexture tex = entry.getValue().get(0);
            binMips[entry.getKey()] = new TextureBinMips(tex.getData(), this.tileSize, this.tileSize); 
            Engine.checkGLError("GL12.glTexSubImage3D");
            uploadprogress = ++nBlock / (float) totalTex;
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
        int len = 64;
        for (int i = 0; i < len; i++) {
            String path = "textures/noise/LDR_RGBA_" + i + ".png";
            AssetTexture tex = mgr.loadPNGAsset(path, false);
            blockIDToAssetList.put(i, Lists.newArrayList(tex));
            texNameToAssetMap.put(path, tex);
            loadprogress = (i / (float) len);
        }
    }

}
